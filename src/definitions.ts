export interface NextcloudSsoPlugin {
  chooseAccount(): Promise<SingleSignOnAccount>;
}

export interface SingleSignOnAccount {
  name: string;
  userId: string;
  token: string;
  url: string;
  type: string;
}