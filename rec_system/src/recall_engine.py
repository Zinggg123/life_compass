import faiss
import numpy as np
import threading
from concurrent.futures import ThreadPoolExecutor

class RecallEngine:
    def __init__(self, lgcn_wrapper, sasrec_wrapper, lgcn_index_path, sasrec_index_path, hot_item_ids=None, top_k=100):
        self.lgcn_model = lgcn_wrapper
        self.sasrec_model = sasrec_wrapper
        
        # 加载 Faiss 索引
        self.lgcn_index = faiss.read_index(lgcn_index_path)
        self.sasrec_index = faiss.read_index(sasrec_index_path)
        
        self.top_k = top_k
        self.hot_item_ids = hot_item_ids or {}
        self.executor = ThreadPoolExecutor(max_workers=4)

        # print(self.hot_item_ids)

    def _search_lgcn(self, user_vector, requested_k):
        # Faiss 搜索
        D, I = self.lgcn_index.search(user_vector, requested_k)
        return {"source": "LightGCN", "scores": D[0].tolist(), "item_ids": I[0].tolist()}

    def _search_sasrec(self, user_vector, requested_k):
        # Faiss 搜索
        D, I = self.sasrec_index.search(user_vector, requested_k)
        return {"source": "SASRec", "scores": D[0].tolist(), "item_ids": I[0].tolist()}
        
    def _search_hot(self, requested_k):
        # 热门召回通道
        k = min(requested_k, len(self.hot_item_ids["item_ids"]))
        # print(f"Hot召回通道召回{k}个item")
        return {"source": "Hot", "scores": self.hot_item_ids["scores"][:k], "item_ids": self.hot_item_ids["item_ids"][:k]}
        # 分数均为0

    def parallel_recall(self, vec_lgcn, vec_sas, target_k=None):
        future_lgcn = None
        future_sasrec = None
        future_hot = None
        
        k = target_k if target_k else self.top_k

        if vec_lgcn is not None:
            if vec_lgcn.ndim == 1:
                vec_lgcn = vec_lgcn.reshape(1, -1)
            future_lgcn = self.executor.submit(self._search_lgcn, vec_lgcn, k)

        if vec_sas is not None:
            if vec_sas.ndim == 1:
                vec_sas = vec_sas.reshape(1, -1)
            future_sasrec = self.executor.submit(self._search_sasrec, vec_sas, k)
            
        future_hot = self.executor.submit(self._search_hot, k)
        
        results = []
        try:
            if future_lgcn is not None:
                results.append(future_lgcn.result())
            if future_sasrec is not None:
                results.append(future_sasrec.result())
            if future_hot is not None:
                results.append(future_hot.result())
        except Exception as e:
            print(f"Recall error: {e}")
            
        return results