export interface User {
  id?: number;
  username: string;
  nickname?: string;
  email: string;
  phone?: string;
  avatar?: string;
  role: string;
  department?: string;
  status: number; // 0-正常 1-禁用
  createdTime?: string;
  updatedTime?: string;
}

export interface UserQuery {
  keyword?: string;
  pageNum: number;
  pageSize: number;
  role?: string;
  status?: number;
}

export interface LoginForm {
  username: string;
  password: string;
  remember?: boolean;
}

export interface RegisterForm {
  username: string;
  password: string;
  confirmPassword: string;
  email: string;
  nickname?: string;
  phone?: string;
}

export interface PasswordForm {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface PageResult<T> {
  total: number;
  list: T[];
} 