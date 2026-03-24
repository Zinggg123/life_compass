import numpy as np
import torch

class SimpleRanker:
    def __init__(self, weights=None, final_top_k=100, gru4rec_model=None):
        # 权重：{'LightGCN': 0.5, 'SASRec': 0.5}
        self.weights = weights or {'LightGCN': 0.5, 'SASRec': 0.5}
        self.final_top_k = final_top_k
        self.gru4rec_model = gru4rec_model

    def sort(self, recall_results):
        """
        recall_results: list of dict [{'source':..., 'scores':..., 'item_ids':...}, ...]
        """
        score_map = {} # item_id -> weighted_score


        for res in recall_results:
            source = res['source']
            w = self.weights.get(source, 0.0)
            
            # print(f"Processing {source} results with weight {w}:")
            for score, item_id in zip(res['scores'], res['item_ids']):
                # 过滤掉 Faiss 返回的 -1 (无效 ID)
                if item_id == -1: continue
                
                # 简单加权累加 (如果同一物品被多路召回，分数叠加)
                if item_id not in score_map:
                    score_map[item_id] = 0.0
                score_map[item_id] += score * w
                # print(f"  {item_id}: {score * w}")
        
        # 排序
        sorted_items = sorted(score_map.items(), key=lambda x: x[1], reverse=True)
        
        # 截取 Top K
        final_ids = [item_id for item_id, _ in sorted_items[:self.final_top_k]]
        final_scores = [score for _, score in sorted_items[:self.final_top_k]]
        
        return final_ids, final_scores
    
    def sort_with_gru4rec(self, recall_results, user_gru_vector):
        """
        使用 GRU4Rec 模型对召回结果进行重排。
        recall_results: 多路召回的结果列表
        user_gru_vector: 当前用户过 GRU4Rec 模型后输出的向量 (numpy array 或 tensor)
        """
        if self.gru4rec_model is None:
            raise ValueError("GRU4Rec model was not initialized in Ranker.")

        # 1. 收集所有召回候选物品去重
        candidate_item_ids = set()
        for res in recall_results:
            for item_id in res['item_ids']:
                if item_id != -1: # 过滤无效 ID
                    candidate_item_ids.add(int(item_id))
        
        candidate_item_ids = list(candidate_item_ids)
        if not candidate_item_ids:
            return [], []

        # 2. 准备设备和 Tensor
        device = self.gru4rec_model.device
        items_tensor = torch.tensor(candidate_item_ids, dtype=torch.long, device=device)
        
        if isinstance(user_gru_vector, np.ndarray):
            user_tensor = torch.tensor(user_gru_vector, dtype=torch.float32, device=device)
        else:
            user_tensor = user_gru_vector.clone().detach().to(device)

        if user_tensor.ndim == 1:
            user_tensor = user_tensor.unsqueeze(0)

        # 3. 获取候选物品 Embedding 并计算点乘得分
        self.gru4rec_model.model.eval()
        with torch.no_grad():
            item_embs = self.gru4rec_model.model.item_embedding(items_tensor) # (num_candidates, dim)
            scores = torch.matmul(user_tensor, item_embs.transpose(0, 1)).squeeze(0) # (num_candidates,)
            scores_np = scores.cpu().numpy()

        # 4. 组装结果并排序
        score_map = {item_id: score for item_id, score in zip(candidate_item_ids, scores_np)}
        sorted_items = sorted(score_map.items(), key=lambda x: x[1], reverse=True)
        
        # 5. 截取 Top K
        final_ids = [item_id for item_id, _ in sorted_items[:self.final_top_k]]
        final_scores = [float(score) for _, score in sorted_items[:self.final_top_k]]
        
        return final_ids, final_scores