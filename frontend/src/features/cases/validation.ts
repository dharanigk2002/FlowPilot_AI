import { z } from "zod";

export const createCaseSchema = z.object({
  title: z.string().trim().min(1, "Title is required.").max(160),
  description: z.string().trim().min(1, "Description is required.").max(5000),
  customerName: z.string().trim().min(1, "Customer name is required.").max(120),
  customerEmail: z.union([z.literal(""), z.string().trim().email("Enter a valid email address.").max(320)]),
  customerTier: z.enum(["STANDARD", "PREMIUM"]),
  orderReference: z.string().trim().max(80),
  orderValue: z.union([z.literal(""), z.string().regex(/^\d{1,10}(\.\d{1,2})?$/, "Enter a positive amount with at most two decimals.")]),
  priority: z.enum(["LOW", "MEDIUM", "HIGH", "URGENT"]),
});

export type CreateCaseValues = z.infer<typeof createCaseSchema>;
