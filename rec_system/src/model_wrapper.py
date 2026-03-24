import torch
from recbole.utils import get_model
from recbole.config import Config
import os

class ModelWrapper:
    def __init__(self, model_name, config_file, pth_path):
        self.model_name = model_name
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        
        # 初始化配置 (仅为了构建模型架构，不需要 dataset 对象参与推理逻辑，但需要 config)
        # 注意：这里需要一个 dummy dataset 或者加载之前的 dataset 结构来初始化模型
        # 为了简化工程，我们假设 config 里包含了所有必要参数，且模型初始化不依赖 dataset 的具体交互数据
        # 实际上 RecBole 模型初始化需要 dataset 来获取 num_users/items。
        # 【工程技巧】：我们可以创建一个极简的 Mock Dataset 类，或者重新加载一次小数据集
        # 这里为了代码简洁，假设我们通过 config 能恢复，或者你需要在这里重新 load 一次 dataset (参考 prepare_artifacts)
        # 在实际生产中，建议把 num_users, num_items 也存到 meta.pkl 里，这里手动构造一个简易 dataset 对象传给模型 init
        
        # 由于 RecBole 强依赖 dataset 初始化，这里简化处理：
        # 重新加载一次 dataset (只读 meta，不加载全部交互，速度快)
        from recbole.data import create_dataset
        config = Config(model=model_name, config_file_list=[config_file])
        dataset = create_dataset(config) # 这会读取.inter 文件，如果文件大会有开销。
        # 优化方案：修改 RecBole 源码或自定义一个只含 num_users/items 的 DummyDataset
        
        self.model = get_model(config['model'])(config, dataset).to(self.device)
        checkpoint = torch.load(pth_path, map_location=self.device, weights_only=False)
        self.model.load_state_dict(checkpoint['state_dict'])
        self.model.eval()
        
        self.embedding_dim = config['embedding_size']


        
    def compute_user_vector(self, input_data):
        """
        输入: 
           - LightGCN: user_idx_tensor (1,)
           - SASRec: (seq_tensor, seq_len_tensor)
        输出: user_vector (1, dim) numpy array
        """
        with torch.no_grad():
            if self.model_name == "LightGCN":
                user_idx = input_data
                vec = self.model.user_embedding(user_idx)
                
            elif self.model_name == "SASRec":
                seq, seq_len = input_data
                # 复用 SASRec 的前向逻辑提取 seq_output 的最后一位
                # 注意：RecBole 的 SASRec forward 通常返回 scores，我们需要中间层
                # 这里手动复现 forward 的关键部分以获取 embedding
                
                masked_emb = self.model.item_embedding(seq)
                position_ids = torch.arange(seq.size(1), dtype=torch.long, device=self.device)
                position_ids = position_ids.unsqueeze(0).expand_as(seq)
                position_emb = self.model.position_embedding(position_ids)
                
                seq_output = masked_emb + position_emb
                
                # Attention Mask
                att_mask = (seq != 0)
                att_mask = att_mask.unsqueeze(1).unsqueeze(2)
                att_mask = (~att_mask).float() * -10000.0
                
                # # Transformer Layers
                for layer in self.model.trm_encoder.layer:
                    seq_output = layer(seq_output, att_mask)
            
                # Gather last valid item
                last_pos = seq_len - 1
                last_pos = torch.clamp(last_pos, min=0) # 处理 seq_len=0 的情况
                
                user_vec = seq_output.gather(1, last_pos.view(-1, 1, 1).expand(-1, -1, seq_output.shape[-1])).squeeze(1)
                vec = user_vec
                
            elif self.model_name == "GRU4Rec":
                seq, seq_len = input_data
                item_seq_emb = self.model.item_embedding(seq)
                item_seq_emb_dropout = self.model.emb_dropout(item_seq_emb)
                
                # GRU前向传播
                gru_output, _ = self.model.gru_layers(item_seq_emb_dropout)
                gru_output = self.model.dense(gru_output)
                
                # 获取最后一次有效点击的隐向量
                last_pos = seq_len - 1
                last_pos = torch.clamp(last_pos, min=0)
                
                user_vec = gru_output.gather(1, last_pos.view(-1, 1, 1).expand(-1, -1, gru_output.shape[-1])).squeeze(1)
                vec = user_vec
            
            return vec.detach().cpu().numpy()