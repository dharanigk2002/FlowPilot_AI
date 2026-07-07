"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FileCheck2, FileWarning, LoaderCircle, UploadCloud } from "lucide-react";
import { useRef, useState, type ChangeEvent } from "react";

import { Button } from "@/components/ui/button";
import { EmptyState, ErrorAlert, LoadingState } from "@/components/ui/feedback";
import { Pagination } from "@/components/ui/pagination";
import { ApiClientError } from "@/lib/api/client";
import type { DocumentStatus } from "@/lib/api/types";
import { cn, formatDate, formatFileSize } from "@/lib/utils";
import { useAppSelector } from "@/store/hooks";

import { getDocuments, uploadDocument } from "../api";

const allowedTypes = new Set(["application/pdf", "text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"]);
const statusStyle: Record<DocumentStatus, string> = { PROCESSING: "bg-blue-50 text-blue-700", READY: "bg-emerald-50 text-emerald-700", FAILED: "bg-rose-50 text-rose-700" };

export function DocumentManager() {
  const [page, setPage] = useState(0);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const role = useAppSelector((state) => state.auth.user?.role);
  const queryClient = useQueryClient();
  const documents = useQuery({ queryKey: ["documents", page], queryFn: () => getDocuments(page) });
  const upload = useMutation({ mutationFn: uploadDocument, onSuccess: async () => { setSelectedFile(null); if (inputRef.current) inputRef.current.value = ""; await queryClient.invalidateQueries({ queryKey: ["documents"] }); } });

  function selectFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    setFileError(null);
    if (!file) return setSelectedFile(null);
    if (!allowedTypes.has(file.type)) return setFileError("Choose a PDF, DOCX, or plain-text document.");
    if (file.size > 10 * 1024 * 1024) return setFileError("The document must not exceed 10 MB.");
    setSelectedFile(file);
  }

  return (
    <div className="space-y-7">
      {role === "ADMIN" ? <section className="rounded-xl border border-indigo-200 bg-indigo-50/60 p-5"><div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between"><div><h2 className="font-semibold text-slate-950">Add policy document</h2><p className="mt-1 text-sm text-slate-600">PDF, DOCX, or TXT up to 10 MB. Text is extracted and indexed locally.</p></div><div className="flex flex-col gap-2 sm:min-w-80"><input ref={inputRef} type="file" accept=".pdf,.docx,.txt,application/pdf,text/plain,application/vnd.openxmlformats-officedocument.wordprocessingml.document" onChange={selectFile} className="block w-full text-sm text-slate-600 file:mr-3 file:rounded-lg file:border-0 file:bg-white file:px-3 file:py-2 file:font-semibold file:text-indigo-700 hover:file:bg-indigo-100" />{fileError ? <p className="text-sm text-rose-600">{fileError}</p> : null}<Button disabled={!selectedFile || upload.isPending} onClick={() => selectedFile && upload.mutate(selectedFile)}>{upload.isPending ? <LoaderCircle className="size-4 animate-spin" /> : <UploadCloud className="size-4" />}{upload.isPending ? "Processing..." : "Upload and index"}</Button></div></div>{upload.error ? <div className="mt-4"><ErrorAlert message={upload.error instanceof ApiClientError ? upload.error.message : "The document could not be uploaded."} /></div> : null}</section> : null}
      {documents.isPending ? <LoadingState label="Loading documents" /> : documents.isError ? <ErrorAlert message="Documents could not be loaded." /> : documents.data.items.length === 0 ? <EmptyState title="No policy documents" description={role === "ADMIN" ? "Upload a company policy to make it available for semantic search." : "An administrator has not uploaded any policies yet."} /> : <><div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{documents.data.items.map((document) => <article key={document.id} className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm"><div className="flex items-start justify-between gap-3"><span className={cn("rounded-lg p-2", document.status === "FAILED" ? "bg-rose-50 text-rose-600" : "bg-indigo-50 text-indigo-600")}>{document.status === "FAILED" ? <FileWarning className="size-5" /> : <FileCheck2 className="size-5" />}</span><span className={cn("rounded-full px-2.5 py-1 text-xs font-semibold", statusStyle[document.status])}>{document.status}</span></div><h3 className="mt-4 break-words font-semibold text-slate-900">{document.fileName}</h3><div className="mt-3 space-y-1 text-xs text-slate-500"><p>{formatFileSize(document.sizeBytes)} · {document.chunkCount} chunks</p><p>Uploaded {formatDate(document.createdAt)}</p><p>By {document.uploadedBy.displayName}</p></div>{document.errorMessage ? <p className="mt-4 rounded-lg bg-rose-50 p-3 text-xs text-rose-700">{document.errorMessage}</p> : null}</article>)}</div><Pagination page={documents.data.page} totalPages={documents.data.totalPages} onPageChange={setPage} /></>}
    </div>
  );
}
