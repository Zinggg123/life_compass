import faiss
import numpy as np
import threading
from concurrent.futures import ThreadPoolExecutor

class RecallEngine:
    def __init__(self, lgcn_wrapper, sasrec_wrapper, lgcn_index_path, sasrec_index_path, top_k=50):
        self.lgcn_model = lgcn_wrapper
        self.sasrec_model = sasrec_wrapper
        
        # 加载 Faiss 索引
        self.lgcn_index = faiss.read_index(lgcn_index_path)
        self.sasrec_index = faiss.read_index(sasrec_index_path)
        
        self.top_k = top_k
        self.executor = ThreadPoolExecutor(max_workers=4)

    def _search_lgcn(self, user_vector):
        # Faiss 搜索
        D, I = self.lgcn_index.search(user_vector, self.top_k)
        return {"source": "LightGCN", "scores": D[0], "item_ids": I[0]}

    def _search_sasrec(self, user_vector):
        # Faiss 搜索
        D, I = self.sasrec_index.search(user_vector, self.top_k)
        return {"source": "SASRec", "scores": D[0], "item_ids": I[0]}

    def parallel_recall(self, vec_lgcn, vec_sas):
        future_lgcn = None
        future_sasrec = None

        if vec_lgcn is not None:
            if vec_lgcn.ndim == 1:
                vec_lgcn = vec_lgcn.reshape(1, -1)
            future_lgcn = self.executor.submit(self._search_lgcn, vec_lgcn)

        if vec_sas is not None:
            if vec_sas.ndim == 1:
                vec_sas = vec_sas.reshape(1, -1)
            future_sasrec = self.executor.submit(self._search_sasrec, vec_sas)
        
        results = []
        try:
            if future_lgcn is not None:
                results.append(future_lgcn.result())
            if future_sasrec is not None:
                results.append(future_sasrec.result())
        except Exception as e:
            print(f"Recall error: {e}")
            
        return results