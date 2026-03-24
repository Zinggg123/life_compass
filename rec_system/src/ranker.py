import numpy as np
import torch

class SimpleRanker:
    def __init__(self, weights=None, final_top_k=100, gru4rec_model=None):
        # 权重：{'LightGCN': 0.5, 'SASRec': 0.5}
        self.weights = weights or {'LightGCN': 0.5, 'SASRec': 0.5}
        self.final_top_k = final_top_k
        self.gru4rec_model = gru4rec_model

    def rank(self, recall_results, user_gru_vector=None, topk=None):
        """
        统一的排序接口
        """

        k = topk if topk else self.final_top_k

        if user_gru_vector is not None and self.gru4rec_model is not None:
            print("Using GRU4Rec model to sort")
            return self.sort_with_gru4rec(recall_results, user_gru_vector, k)
        else:
            print("Using weighted sum to sort")
            return self.sort(recall_results, k)

    def sort(self, recall_results, k):
        """
        降级方案：合并结果传入的是已经排好序（个性化前置，热门打底）的列表字典
        格式: [{'item_id': id, 'scores': {'LightGCN': s1, 'SASRec': s2, 'HotLevel': s3}}, ...]
        """
        if not recall_results:
            return [], []
            
        score_map = {} # item_id -> weighted_score

        for res in recall_results:
            item_id = res['item_id']
            scores = res.get('scores', {})
            
            # 计算个性化通道平均分
            pers_scores = []
            if 'LightGCN' in scores: pers_scores.append(scores['LightGCN'])
            if 'SASRec' in scores: pers_scores.append(scores['SASRec'])
            
            # pers_score = sum(pers_scores) / len(pers_scores) if pers_scores else 0.0
            pers_score = sum(pers_scores) / 2 if pers_scores else 0.0 # 两个模型分数均分(缺失时也均分)
            hot_score = scores.get('Hot', 0.0)
            
            # 加权融合: 个性化 0.9, 热门 0.1
            final_score = 0.9 * pers_score + 0.1 * hot_score
            score_map[item_id] = final_score
        
        # 排序
        sorted_items = sorted(score_map.items(), key=lambda x: x[1], reverse=True)
        
        # 截取 Top K
        final_ids = [item_id for item_id, _ in sorted_items[:k]]
        final_scores = [score for _, score in sorted_items[:k]]
        
        return final_ids, final_scores
    
    def sort_with_gru4rec(self, recall_results, user_gru_vector, k):
        """
        使用 GRU4Rec 模型对召回结果进行重排。
        recall_results: [{'item_id': id, 'scores': {...}}, ...]
        user_gru_vector: 当前用户过 GRU4Rec 模型后输出的向量 (numpy array 或 tensor)
        """
        if self.gru4rec_model is None:
            raise ValueError("GRU4Rec model was not initialized in Ranker.")

        if not recall_results:
            return [], []

        candidate_item_ids = [res['item_id'] for res in recall_results]

        # 1. 准备设备和 Tensor
        device = self.gru4rec_model.device
        items_tensor = torch.tensor(candidate_item_ids, dtype=torch.long, device=device)
        
        if isinstance(user_gru_vector, np.ndarray):
            user_tensor = torch.tensor(user_gru_vector, dtype=torch.float32, device=device)
        else:
            user_tensor = user_gru_vector.clone().detach().to(device)

        if user_tensor.ndim == 1:
            user_tensor = user_tensor.unsqueeze(0)

        # 2. 获取候选物品 Embedding 并计算点乘得分
        self.gru4rec_model.model.eval()
        with torch.no_grad():
            item_embs = self.gru4rec_model.model.item_embedding(items_tensor) # (num_candidates, dim)
            scores = torch.matmul(user_tensor, item_embs.transpose(0, 1)).squeeze(0) # (num_candidates,) 点积计算
            scores_np = scores.cpu().numpy()

        # 3. 组装结果并排序
        score_map = {item_id: float(score) for item_id, score in zip(candidate_item_ids, scores_np)}
        sorted_items = sorted(score_map.items(), key=lambda x: x[1], reverse=True)
        
        # 4. 截取 Top K
        final_ids = [item_id for item_id, _ in sorted_items[:k]]
        final_scores = [score for _, score in sorted_items[:k]]
        
        return final_ids, final_scores