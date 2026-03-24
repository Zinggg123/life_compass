import pickle
import torch
import numpy as np
import os
import pandas as pd

# 加载 pkl 元数据，并将 HTTP 传来的字符串 ID 转换为模型需要的 Tensor
# 加载热门榜单
class DataProcessor:
    def __init__(self, meta_path):
        with open(meta_path, 'rb') as f:
            self.meta = pickle.load(f)
        
        self.user_token2id = self.meta['user_token2id']
        self.item_token2id = self.meta['item_token2id']
        self.item_id2token = self.meta['item_id2token']
        self.max_seq_len = self.meta['max_seq_len']
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        
        self.hot_items_internal_ids = {}

    # def load_hot_items(self, csv_file_path):
    #     """加载热门榜并且转换为模型内部ID，便于后续统一处理"""
    #     # 常理而言热门榜单应该不会在recbole中被过滤掉
    #     if os.path.exists(csv_file_path):
    #         df = pd.read_csv(csv_file_path)

    #         scores = []
    #         item_ids = []

    #         for biz_id in df['biz_id']:
    #             iid = self.item_token2id.get(biz_id)
    #             if iid is not None:
    #                 item_ids.append(iid)
    #                 scores.append(df.loc[df['biz_id'] == biz_id, 'final_score'].values[0])
    #             else:
    #                 print(f"biz_id {biz_id} not found in item_token2id")

    #         self.hot_items_internal_ids["scores"] = scores
    #         self.hot_items_internal_ids["item_ids"] = item_ids

    def load_hot_items(self, csv_file_path):
        """
        加载热门榜并转换为模型内部ID，便于后续统一处理
        """
        if not os.path.exists(csv_file_path):
            print(f"热门文件不存在：{csv_file_path}")
            return

        df = pd.read_csv(csv_file_path, usecols=["biz_id", "final_score"])

        # 批量映射 ID
        df["internal_id"] = df["biz_id"].map(self.item_token2id)
        valid_df = df.dropna(subset=["internal_id"])

        self.hot_items_internal_ids["item_ids"] = valid_df["internal_id"].astype(int).tolist()
        self.hot_items_internal_ids["scores"] = valid_df["final_score"].tolist()

        # 打印统计信息
        invalid_num = len(df) - len(valid_df)
        if invalid_num > 0:
            print(f"[热门榜] {invalid_num} 个商家ID在模型中不存在，已过滤")
        print(f"[热门榜] 加载完成，共 {len(valid_df)} 个有效商家")


    def get_user_vector_input(self, model_type, user_id_str, history_items_list):
        """
        根据模型类型准备输入
        返回: (user_internal_id, input_tensor_for_model)
        """
        # 1. 转换 User ID
        u_idx = self.user_token2id.get(user_id_str)
        
        if model_type == "LightGCN":
            return self._prepare_lightgcn_input(u_idx)
        elif model_type in ["SASRec", "GRU4Rec"]:
            return self._prepare_sequential_input(u_idx, history_items_list, model_type)
        
        return None, None

    def _prepare_lightgcn_input(self, u_idx):
        print(f"LightGCN input: {u_idx}")
        if u_idx is None:
            return None, None
        # LightGCN 只需要 user_idx 来查表， 历史行为向量返回空值
        return u_idx, torch.tensor([u_idx], dtype=torch.long, device=self.device)

    def _prepare_sequential_input(self, u_idx, history_items_list, model_type):
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
            
        if len(item_ids) > self.max_seq_len:
            seq = item_ids[-self.max_seq_len:]
        else:
            seq = [0] * (self.max_seq_len - len(item_ids)) + item_ids
        seq_len = min(len(item_ids), self.max_seq_len)
        
        seq_tensor = torch.tensor([seq], dtype=torch.long, device=self.device)
        seq_len_tensor = torch.tensor([seq_len], dtype=torch.long, device=self.device)
        
        return u_idx, (seq_tensor, seq_len_tensor)

    def map_item_ids_to_tokens(self, item_ids):
        return [self.item_id2token.get(idx, "UNKNOWN") for idx in item_ids]