import torch
import numpy as np
import pickle
import faiss
import os
import warnings

# 直接忽略相关警告，希望 RecBole 内部逻辑能兼容
warnings.filterwarnings('ignore', category=FutureWarning)
warnings.filterwarnings('ignore', category=UserWarning)
warnings.filterwarnings('ignore', message=".*Copy-on-Write.*")

from recbole.config import Config
from recbole.data import create_dataset
from recbole.utils import get_model

# 配置路径
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(BASE_DIR)

ARTIFACTS_DIR = os.path.join(PROJECT_ROOT, "artifact")
CONFIG_DIR = os.path.join(PROJECT_ROOT, "config")

CONFIG_FILES = {
    "LightGCN": os.path.join(CONFIG_DIR, "lightgcn_config.yaml"),
    "SASRec": os.path.join(CONFIG_DIR, "sasrec_config.yaml")
}

MODEL_PATHS = {
    "LightGCN": os.path.join(ARTIFACTS_DIR, "LightGCN-Mar-11-2026_15-43-30.pth"),
    "SASRec": os.path.join(ARTIFACTS_DIR, "SASRec-Mar-12-2026_16-53-02.pth")
}


def export_artifacts(model_name, config_file, model_path):
    print(f"--- Processing {model_name} ---")
    
    if not os.path.exists(config_file):
        raise FileNotFoundError(f"Config file not found: {config_file}")
    if not os.path.exists(model_path):
        raise FileNotFoundError(f"Model file not found: {model_path}")

    # 1. 初始化配置和数据集
    config = Config(model=model_name, config_file_list=[config_file])
    dataset = create_dataset(config)
    
    num_users = len(dataset.field2id_token['user_id'])
    num_items = len(dataset.field2id_token['item_id'])

    # 2. 保存元数据 (ID 映射)
    def build_mappings(dataset, field):
        tokens_list = dataset.field2id_token.get(field, [])
        
        if len(tokens_list) == 0:
            raise ValueError(f"No tokens found for field '{field}'.")

        id2token = {idx: token for idx, token in enumerate(tokens_list)}
        token2id = {token: idx for idx, token in enumerate(tokens_list)}
        
        return token2id, id2token

    user_t2i, user_i2t = build_mappings(dataset, 'user_id')
    item_t2i, item_i2t = build_mappings(dataset, 'item_id')

    meta_data = {
        "user_token2id": user_t2i,
        "user_id2token": user_i2t,
        "item_token2id": item_t2i,
        "item_id2token": item_i2t,
        "max_seq_len": getattr(config, 'MAX_ITEM_LIST_LENGTH', 50), #TODO: bug:meta.pkl不应包含
        "embedding_dim": config['embedding_size'], #TODO: bug:meta.pkl不应包含
        "num_users": num_users, 
        "num_items": num_items
    }
    
    os.makedirs(ARTIFACTS_DIR, exist_ok=True)
    meta_path = os.path.join(ARTIFACTS_DIR, "dataset_meta.pkl")
    with open(meta_path, 'wb') as f:
        pickle.dump(meta_data, f)
    print(f"Saved meta data to {meta_path}")

    # 3. 加载模型权重
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    
   
    # 注意：某些 RecBole 版本初始化模型时需要 dataset 中的 num_users/num_items
    # 如果模型初始化失败，可能需要手动将这些属性注入 dataset 对象
    dataset.num_users = num_users
    dataset.num_users = num_users
    
    # 初始化模型
    model = get_model(config['model'])(config, dataset).to(device)
    checkpoint = torch.load(model_path, map_location=device, weights_only=False)
    model.load_state_dict(checkpoint['state_dict'])
    model.eval()
    print(f"Model weights loaded.")

    # 4. 提取 Item Embeddings
    # 使用我们计算出的 num_items 来生成 ID 序列
    item_ids = torch.arange(num_items, dtype=torch.long, device=device)
    print(f"Extracting embeddings for {num_items} items...")
    with torch.no_grad():
        if model_name == "LightGCN":
            item_embs = model.item_embedding(item_ids)
                
        elif model_name == "SASRec":
            item_embs = model.item_embedding(item_ids)
        else:
            raise ValueError(f"Unsupported model: {model_name}")
            
        item_embs_np = item_embs.detach().cpu().numpy().astype('float32')


    # 5. 构建 Faiss 索引并保存
    dimension = item_embs_np.shape[1]
    index = faiss.IndexFlatIP(dimension) 
    index.add(item_embs_np)
    
    faiss_path = os.path.join(ARTIFACTS_DIR, f"{model_name.lower()}_faiss.index")
    faiss.write_index(index, faiss_path)
    print(f"Saved Faiss index to {faiss_path}")
    
    npy_path = os.path.join(ARTIFACTS_DIR, f"{model_name.lower()}_item_embs.npy")
    np.save(npy_path, item_embs_np)
    print(f"Saved item embeddings to {npy_path}")



if __name__ == "__main__":
    try:
        export_artifacts("LightGCN", CONFIG_FILES["LightGCN"], MODEL_PATHS["LightGCN"])
        export_artifacts("SASRec", CONFIG_FILES["SASRec"], MODEL_PATHS["SASRec"])
        print("\n=== All Artifacts Prepared Successfully ===")
    except Exception as e:
        print(f"\n=== Error Occurred: {e} ===")