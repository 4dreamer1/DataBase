export interface Equipment {
  id?: number;
  code: string;
  name: string;
  categoryId: number;
  categoryName?: string;
  category?: Category;
  status: number;
  statusName?: string;
  specification?: string;
  description?: string;
  imageUrl?: string;
  manufacturer?: string;
  purchaseDate?: string;
  price?: number;
  createTime?: string;
  updateTime?: string;
}

export interface EquipmentQuery {
  keyword?: string;
  pageNum: number;
  pageSize: number;
  categoryId?: number;
  status?: number;
  sortBy?: string;
  sortOrder?: string;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  createTime?: string;
  updateTime?: string;
  equipmentCount?: number;
}

export interface PageResult<T> {
  total: number;
  list: T[];
  pageNum?: number;
  pageSize?: number;
  pages?: number;
} 