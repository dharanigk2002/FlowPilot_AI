import { ChevronLeft, ChevronRight } from "lucide-react";

import { Button } from "./button";

export function Pagination({ page, totalPages, onPageChange }: { page: number; totalPages: number; onPageChange: (page: number) => void }) {
  if (totalPages <= 1) return null;
  return <div className="flex items-center justify-between border-t border-slate-200 pt-4"><p className="text-sm text-slate-500">Page <span className="font-medium text-slate-800">{page + 1}</span> of {totalPages}</p><div className="flex gap-2"><Button variant="secondary" className="px-3" disabled={page === 0} onClick={() => onPageChange(page - 1)}><ChevronLeft className="size-4" /> Previous</Button><Button variant="secondary" className="px-3" disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}>Next <ChevronRight className="size-4" /></Button></div></div>;
}
