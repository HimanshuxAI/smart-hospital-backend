export type FieldErrors = Record<string, string>;

export function validateEmail(email: string): string | null {
  if (!email || !email.trim()) {
    return "Email is required";
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
    return "Invalid email address";
  }
  return null;
}

export function validatePassword(password: string): string | null {
  if (!password) {
    return "Password is required";
  }
  if (password.length < 8) {
    return "Password must be at least 8 characters";
  }
  return null;
}

export function validateRequired(val: string, fieldName: string): string | null {
  if (!val || !val.trim()) {
    return `${fieldName} is required`;
  }
  return null;
}
