export interface MorphoUsbPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
