export interface NextcloudSsoPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
