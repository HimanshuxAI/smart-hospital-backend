export function Avatar({
  name,
  src,
  size = "md",
}: {
  name: string;
  src?: string;
  size?: "sm" | "md" | "lg";
}) {
  const initials = name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");

  if (src) {
    return <img className={`avatar avatar-${size}`} src={src} alt={name} />;
  }

  return (
    <span className={`avatar avatar-${size}`} role="img" aria-label={name}>
      {initials}
    </span>
  );
}
