import numpy as np

class SimpleRanker:
    def __init__(self, weights=None, final_top_k=10):
        # 权重：{'LightGCN': 0.5, 'SASRec': 0.5}
        self.weights = weights or {'LightGCN': 0.5, 'SASRec': 0.5}
        self.final_top_k = final_top_k

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