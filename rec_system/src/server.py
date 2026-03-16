from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import os
import sys
import warnings

# 直接忽略相关警告，希望 RecBole 内部逻辑能兼容
warnings.filterwarnings('ignore', category=FutureWarning)
warnings.filterwarnings('ignore', category=UserWarning)
warnings.filterwarnings('ignore', message=".*Copy-on-Write.*")

# 引入上述模块
from src.data_loader import DataProcessor
from src.model_wrapper import ModelWrapper
from src.recall_engine import RecallEngine
from src.ranker import SimpleRanker

app = FastAPI(title="Realtime RecSys")

# --- 全局初始化 (启动时加载一次) ---
# BASE_DIR = os.path.dirname(os.path.abspath(__file__))
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ARTIFACTS_DIR = os.path.join(BASE_DIR, "artifact")
CONFIG_DIR = os.path.join(BASE_DIR, "config")

# 1. 加载数据处理器
processor = DataProcessor(os.path.join(ARTIFACTS_DIR, "dataset_meta.pkl"))

# 2. 加载模型 Wrapper
lgcn_wrapper = ModelWrapper(
    "LightGCN", 
    os.path.join(CONFIG_DIR, "lightgcn_config.yaml"),
    os.path.join(ARTIFACTS_DIR, "LightGCN-Mar-11-2026_15-43-30.pth")
)
sasrec_wrapper = ModelWrapper(
    "SASRec", 
    os.path.join(CONFIG_DIR, "sasrec_config.yaml"),
    os.path.join(ARTIFACTS_DIR, "SASRec-Mar-12-2026_16-53-02.pth")
)

# 3. 初始化召回引擎
engine = RecallEngine(
    lgcn_wrapper, sasrec_wrapper,
    os.path.join(ARTIFACTS_DIR, "lightgcn_faiss.index"),
    os.path.join(ARTIFACTS_DIR, "sasrec_faiss.index"),
    top_k=50
)

# 4. 初始化排序器
ranker = SimpleRanker(weights={'LightGCN': 0.4, 'SASRec': 0.6}, final_top_k=10)

# --- Request Model ---
class RecRequest(BaseModel):
    user_id: str
    history_items: List[str] # 按时间顺序

class RecResponse(BaseModel):
    user_id: str
    recommendations: List[str]
    scores: List[float]

@app.post("/recommend", response_model=RecResponse)
async def recommend(req: RecRequest):
    # 1. 数据预处理
    # LightGCN 输入
    u_idx_lgcn, input_lgcn = processor.get_user_vector_input("LightGCN", req.user_id, req.history_items)
    # SASRec 输入
    u_idx_sas, input_sas = processor.get_user_vector_input("SASRec", req.user_id, req.history_items)
    
    if input_lgcn is None and input_sas is None:
        # TODO:冷启动策略：返回热门物品
        raise HTTPException(status_code=400, detail="Unknown user and empty history")

    # 2. 计算用户向量
    vec_lgcn = lgcn_wrapper.compute_user_vector(input_lgcn) if input_lgcn is not None else None
    vec_sas = sasrec_wrapper.compute_user_vector(input_sas) if input_sas is not None else None

    # 3. 召回
    results = engine.parallel_recall(vec_lgcn, vec_sas)

    if not results:
        raise HTTPException(status_code=500, detail="Failed to generate any recall vectors")

    # 4. 排序
    final_ids, final_scores = ranker.sort(results)
    
    # 5. ID 转 Token
    final_tokens = processor.map_item_ids_to_tokens(final_ids)

    return RecResponse(
        user_id=req.user_id,
        recommendations=final_tokens,
        scores=final_scores
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)