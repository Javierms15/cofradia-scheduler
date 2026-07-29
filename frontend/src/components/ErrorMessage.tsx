export function ErrorMessage({ mensaje }: { mensaje: string | null }) {
  if (!mensaje) return null;
  return <p className="error-message">{mensaje}</p>;
}
