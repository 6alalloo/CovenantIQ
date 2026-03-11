export type UserRole = "ANALYST" | "RISK_LEAD" | "ADMIN";

export type AuthSession = {
  accessToken: string;
  refreshToken: string;
  username: string;
  roles: UserRole[];
};

export type UserResponse = {
  id: number;
  username: string;
  email: string;
  active: boolean;
  roles: UserRole[];
  createdAt: string;
};
