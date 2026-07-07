import { z } from "zod";

export const loginSchema = z.object({
  email: z.string().trim().email("Enter a valid email address."),
  password: z.string().min(8, "Password must contain at least 8 characters.").max(100),
});

export const registerSchema = loginSchema.extend({
  displayName: z.string().trim().min(2, "Display name must contain at least 2 characters.").max(120),
});

export type LoginValues = z.infer<typeof loginSchema>;
export type RegisterValues = z.infer<typeof registerSchema>;
