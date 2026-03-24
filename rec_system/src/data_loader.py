import pickle
import torch
import numpy as np
import os

# 加载 pkl 元数据，并将 HTTP 传来的字符串 ID 转换为模型需要的 Tensor
class DataProcessor:
    def __init__(self, meta_path):
        with open(meta_path, 'rb') as f:
            self.meta = pickle.load(f)
        
        self.user_token2id = self.meta['user_token2id']
        self.item_token2id = self.meta['item_token2id']
        self.item_id2token = self.meta['item_id2token']
        self.max_seq_len = self.meta['max_seq_len']
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

    def get_user_vector_input(self, model_type, user_id_str, history_items_list):
        """
        根据模型类型准备输入
        返回: (user_internal_id, input_tensor_for_model)
        """
        # 1. 转换 User ID
        u_idx = self.user_token2id.get(user_id_str)
        
        if model_type == "LightGCN":
            print(f"LightGCN input: {u_idx}")
            if u_idx is None:
                return None, None
            # LightGCN 只需要 user_idx 来查表， 历史行为向量返回空值
            return u_idx, torch.tensor([u_idx], dtype=torch.long, device=self.device)
            
        elif model_type in ["SASRec", "GRU4Rec"]:
            # 2. 转换 History Items
            item_ids = []
            for item_str in history_items_list:
                i_idx = self.item_token2id.get(item_str)
                if i_idx is not None:
                    item_ids.append(i_idx)

            print(f"{model_type} input: {u_idx}, {item_ids}")

            # SASRec / GRU4Rec 需要序列
            if len(item_ids) == 0: # 空序列处理
                return None, None
            else:
                if len(item_ids) > self.max_seq_len:
                    seq = item_ids[-self.max_seq_len:]
                else:
                    seq = [0] * (self.max_seq_len - len(item_ids)) + item_ids
                seq_len = min(len(item_ids), self.max_seq_len)
            
            seq_tensor = torch.tensor([seq], dtype=torch.long, device=self.device)
            seq_len_tensor = torch.tensor([seq_len], dtype=torch.long, device=self.device)
            
            return u_idx, (seq_tensor, seq_len_tensor)
        
        return None, None

    def map_item_ids_to_tokens(self, item_ids):
        return [self.item_id2token.get(idx, "UNKNOWN") for idx in item_ids]