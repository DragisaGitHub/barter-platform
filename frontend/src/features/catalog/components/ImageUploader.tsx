import { useRef, useState } from "react";
import { Upload, X } from "lucide-react";
import { cn } from "@/utils";
import { useUploadItemImage } from "../useItemImages";

const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp"];
const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
const MAX_IMAGES = 6;

interface ImageUploaderProps {
  itemUuid: string;
  currentImageCount: number;
}

export function ImageUploader({ itemUuid, currentImageCount }: ImageUploaderProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const [progress, setProgress] = useState<number | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);

  const uploadMutation = useUploadItemImage(itemUuid);

  const canUpload = currentImageCount < MAX_IMAGES;

  function validate(file: File): string | null {
    if (!ALLOWED_TYPES.includes(file.type)) {
      return "Only JPEG, PNG, and WebP images are allowed.";
    }
    if (file.size > MAX_FILE_SIZE) {
      return "File must be smaller than 5 MB.";
    }
    if (currentImageCount >= MAX_IMAGES) {
      return `Maximum ${MAX_IMAGES} images allowed per item.`;
    }
    return null;
  }

  function handleFile(file: File) {
    const error = validate(file);
    if (error) {
      setValidationError(error);
      return;
    }
    setValidationError(null);
    setProgress(0);
    uploadMutation.mutate(
      {
        file,
        onProgress: (pct) => setProgress(pct),
      },
      {
        onSettled: () => setProgress(null),
      }
    );
  }

  function handleInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
    // reset so same file can be re-selected
    e.target.value = "";
  }

  function handleDrop(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setIsDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  }

  function handleDragOver(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setIsDragOver(true);
  }

  function handleDragLeave() {
    setIsDragOver(false);
  }

  const isUploading = uploadMutation.isPending;

  return (
    <div className="space-y-2">
      <div
        className={cn(
          "border-2 border-dashed rounded-lg p-6 text-center transition-colors",
          canUpload && !isUploading
            ? "cursor-pointer hover:border-indigo-400 dark:hover:border-indigo-500"
            : "opacity-50 cursor-not-allowed",
          isDragOver
            ? "border-indigo-500 bg-indigo-50 dark:bg-indigo-950/30"
            : "border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-slate-800/50"
        )}
        onDrop={canUpload && !isUploading ? handleDrop : undefined}
        onDragOver={canUpload && !isUploading ? handleDragOver : undefined}
        onDragLeave={canUpload && !isUploading ? handleDragLeave : undefined}
        onClick={() => canUpload && !isUploading && inputRef.current?.click()}
        role={canUpload && !isUploading ? "button" : undefined}
        tabIndex={canUpload && !isUploading ? 0 : undefined}
        onKeyDown={(e) => {
          if ((e.key === "Enter" || e.key === " ") && canUpload && !isUploading) {
            inputRef.current?.click();
          }
        }}
      >
        <input
          ref={inputRef}
          type="file"
          accept={ALLOWED_TYPES.join(",")}
          className="hidden"
          onChange={handleInputChange}
          disabled={!canUpload || isUploading}
        />

        <Upload className="size-8 mx-auto mb-2 text-slate-400 dark:text-slate-500" />

        {isUploading ? (
          <div className="space-y-2">
            <p className="text-sm text-slate-600 dark:text-slate-400">Uploading…</p>
            {progress !== null && (
              <div className="w-full max-w-xs mx-auto h-2 rounded-full bg-slate-200 dark:bg-slate-700 overflow-hidden">
                <div
                  className="h-full bg-indigo-500 transition-all"
                  style={{ width: `${progress}%` }}
                />
              </div>
            )}
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {progress !== null ? `${progress}%` : ""}
            </p>
          </div>
        ) : canUpload ? (
          <>
            <p className="text-sm font-medium text-slate-700 dark:text-slate-300">
              Click to browse or drag & drop
            </p>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
              JPEG, PNG, WebP · max 5 MB · {currentImageCount}/{MAX_IMAGES} images
            </p>
          </>
        ) : (
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Maximum of {MAX_IMAGES} images reached
          </p>
        )}
      </div>

      {validationError && (
        <div className="flex items-start gap-2 text-sm text-red-600 dark:text-red-400">
          <X className="size-4 mt-0.5 shrink-0" />
          <span>{validationError}</span>
        </div>
      )}
    </div>
  );
}

