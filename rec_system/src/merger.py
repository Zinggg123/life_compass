import numpy as np

class Merger:
    def __init__(self):
        pass

    def _softmax_normalize(self, scores):
        """对输入的数值列表进行 softmax 归一化，自带防溢出校验"""
        if not scores or not isinstance(scores, (list, np.ndarray)):
            return []
        
        try:
            scores_arr = np.array(scores, dtype=np.float64)
            # 处理 NaN/Inf 等边界异常问题
            scores_arr = np.nan_to_num(scores_arr, nan=0.0, posinf=0.0, neginf=0.0)
            
            # 最大值作为平移防溢出
            e_x = np.exp(scores_arr - np.max(scores_arr))
            sum_ex = e_x.sum()
            
            # 防除零错
            if sum_ex == 0:
                return [1.0 / len(scores)] * len(scores)
                
            return (e_x / sum_ex).tolist()
        except Exception:
            # 异常时进行托底，归置平分概率
            print("Softmax normalize error, return equal probability")
            return [1.0 / len(scores)] * len(scores)

    def _collect_item_scores(self, recall_results, target_sources):
        """收集所有物品的分数，并对个性化通道实施归一化"""
        item_scores = {}
        for res in recall_results:
            if not isinstance(res, dict):
                print ("Invalid recall result format, expected dict")
                continue
                
            src = res.get('source')
            if not isinstance(src, str):
                print(f"{src}: Invalid recall result format, expected source")
                continue
                
            iids = res.get('item_ids', [])
            scores = res.get('scores', [])
            
            if not isinstance(iids, list) or not isinstance(scores, list):
                print(f"{src}: Invalid recall result format, expected list")
                continue

            # 如果没有分数或者长度不符
            if len(scores) != len(iids):
                print("Invalid recall result format, expected equal length")
                continue
            
            # 若源为个性化通道且存在候选者，则统一进行 softmax 归一化
            if src in target_sources and len(scores) > 0:
                scores = self._softmax_normalize(scores)
                res['scores'] = scores # 回写更新后的分数

            # 合并记录分数，以便后续构建最终结果时使用
            for iid, score in zip(iids, scores):
                try:
                    iid_int = int(iid)
                except (ValueError, TypeError):
                    print("error here:", iid)
                    continue
                    
                if iid_int != -1:
                    if iid_int not in item_scores:
                        item_scores[iid_int] = {}
                    item_scores[iid_int][src] = float(score)
                    
        return item_scores

    def _prioritized_merge(self, recall_results, target_sources, target_k):
        """依据通道优先级，提取并去重物品直至搜集够 target_k 的数量"""
        seen = set()
        final_candidates = []

        def _extract_candidates(source_filter_fn):
            for res in recall_results:
                if not isinstance(res, dict):
                    print ("Invalid recall result format, expected dict")
                    continue
                src = res.get('source')
                if not source_filter_fn(src):
                    continue
                    
                for iid in res.get('item_ids', []):
                    try:
                        iid_int = int(iid)
                    except (ValueError, TypeError):
                        continue
                        
                    if iid_int != -1 and iid_int not in seen:
                        seen.add(iid_int)
                        final_candidates.append(iid_int)
            
            if len(final_candidates) >= target_k:
                return True # 已满
            
            return False

        # 1. 优先提取个性化通道结果
        is_full = _extract_candidates(lambda s: s in target_sources)
        
        # 2. 从热门通道补齐结果
        if not is_full:
            _extract_candidates(lambda s: s not in target_sources)
            
        return final_candidates

    def merge(self, recall_results, target_k):
        """
        按照优先级合并去重并截断，同时保留各路特征分数
        recall_results: list of dict [{'source': '...', 'item_ids': [...], 'scores': [...]}, ...]
        """
        if not isinstance(recall_results, list) or not isinstance(target_k, int) or target_k <= 0:
            print("Invalid arguments")
            return []

        target_sources = {'LightGCN', 'SASRec'}  # 个性化通道

        # 0. 整理和收集所有的候选物品分数
        item_scores = self._collect_item_scores(recall_results, target_sources)

        # print("item_scores:", item_scores)

        # 1-2. 经过优先级去重筛选出符合条件的 item id 列表
        final_candidates = self._prioritized_merge(recall_results, target_sources, target_k)
                    
        # 3. 构造并返回包含分数的最终结果
        top_k_ids = final_candidates[:target_k]
        result = [
            {'item_id': iid, 'scores': item_scores.get(iid, {})}
            for iid in top_k_ids
        ]
        return result
