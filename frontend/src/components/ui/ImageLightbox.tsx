import { useCallback, useEffect } from "react";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import { cn } from "@/utils";

export interface LightboxImage {
  id: string;
  src: string;
  alt: string;
}

interface ImageLightboxLabels {
  title?: string;
  description?: string;
  close?: string;
  previous?: string;
  next?: string;
  counter?: (current: number, total: number) => string;
  thumbnail?: (index: number) => string;
}

interface ImageLightboxProps {
  images: LightboxImage[];
  isOpen: boolean;
  selectedIndex: number;
  onOpenChange: (open: boolean) => void;
  onSelectedIndexChange: (index: number) => void;
  labels?: ImageLightboxLabels;
}

const defaultLabels: Required<ImageLightboxLabels> = {
  title: "Image viewer",
  description: "View item images in fullscreen.",
  close: "Close image viewer",
  previous: "Previous image",
  next: "Next image",
  counter: (current, total) => `${current} of ${total}`,
  thumbnail: (index) => `Show image ${index}`,
};

export function ImageLightbox({
  images,
  isOpen,
  selectedIndex,
  onOpenChange,
  onSelectedIndexChange,
  labels,
}: ImageLightboxProps) {
  const activeIndex = Math.min(Math.max(selectedIndex, 0), Math.max(images.length - 1, 0));
  const activeImage = images[activeIndex];
  const mergedLabels = {
    title: labels?.title ?? defaultLabels.title,
    description: labels?.description ?? defaultLabels.description,
    close: labels?.close ?? defaultLabels.close,
    previous: labels?.previous ?? defaultLabels.previous,
    next: labels?.next ?? defaultLabels.next,
    counter: labels?.counter ?? defaultLabels.counter,
    thumbnail: labels?.thumbnail ?? defaultLabels.thumbnail,
  };
  const hasMultipleImages = images.length > 1;

  const goToIndex = useCallback(
    (nextIndex: number) => {
      if (images.length === 0) {
        return;
      }

      onSelectedIndexChange((nextIndex + images.length) % images.length);
    },
    [images.length, onSelectedIndexChange]
  );

  useEffect(() => {
    if (!isOpen || images.length === 0 || selectedIndex === activeIndex) {
      return;
    }

    onSelectedIndexChange(activeIndex);
  }, [activeIndex, images.length, isOpen, onSelectedIndexChange, selectedIndex]);

  if (images.length === 0) {
    return null;
  }

  return (
    <DialogPrimitive.Root open={isOpen} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-black/85 backdrop-blur-sm data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:animate-in data-[state=open]:fade-in-0" />
        <DialogPrimitive.Content
          className="fixed inset-0 z-50 flex h-dvh w-screen flex-col bg-slate-950/95 p-3 text-white outline-none data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:animate-in data-[state=open]:fade-in-0 sm:p-6"
          onKeyDown={(event) => {
            if (!hasMultipleImages) {
              return;
            }

            if (event.key === "ArrowLeft") {
              event.preventDefault();
              goToIndex(activeIndex - 1);
            }

            if (event.key === "ArrowRight") {
              event.preventDefault();
              goToIndex(activeIndex + 1);
            }
          }}
          onPointerDown={(event) => {
            if (event.target === event.currentTarget) {
              onOpenChange(false);
            }
          }}
        >
          <DialogPrimitive.Title className="sr-only">{mergedLabels.title}</DialogPrimitive.Title>
          <DialogPrimitive.Description className="sr-only">{mergedLabels.description}</DialogPrimitive.Description>

          <div className="flex items-center justify-between gap-3">
            <p className="rounded-full bg-white/10 px-3 py-1 text-sm font-medium text-white/90">
              {mergedLabels.counter(activeIndex + 1, images.length)}
            </p>
            <DialogPrimitive.Close
              className="inline-flex size-10 items-center justify-center rounded-full bg-white/10 text-white transition hover:bg-white/20 focus:outline-none focus-visible:ring-2 focus-visible:ring-white/80"
              aria-label={mergedLabels.close}
            >
              <X className="size-5" aria-hidden="true" />
            </DialogPrimitive.Close>
          </div>

          <div
            className="relative flex min-h-0 flex-1 items-center justify-center py-3 sm:px-14 sm:py-6"
            onPointerDown={(event) => {
              if (event.target === event.currentTarget) {
                onOpenChange(false);
              }
            }}
          >
            {hasMultipleImages ? (
              <button
                type="button"
                onClick={() => goToIndex(activeIndex - 1)}
                className="absolute left-0 top-1/2 z-10 inline-flex size-10 -translate-y-1/2 items-center justify-center rounded-full bg-white/10 text-white transition hover:bg-white/20 focus:outline-none focus-visible:ring-2 focus-visible:ring-white/80 sm:size-12"
                aria-label={mergedLabels.previous}
              >
                <ChevronLeft className="size-6" aria-hidden="true" />
              </button>
            ) : null}

            <img
              src={activeImage.src}
              alt={activeImage.alt}
              className="max-h-[calc(100dvh-8rem)] max-w-full select-none rounded-lg object-contain shadow-2xl sm:max-h-[calc(100dvh-10rem)]"
              draggable={false}
            />

            {hasMultipleImages ? (
              <button
                type="button"
                onClick={() => goToIndex(activeIndex + 1)}
                className="absolute right-0 top-1/2 z-10 inline-flex size-10 -translate-y-1/2 items-center justify-center rounded-full bg-white/10 text-white transition hover:bg-white/20 focus:outline-none focus-visible:ring-2 focus-visible:ring-white/80 sm:size-12"
                aria-label={mergedLabels.next}
              >
                <ChevronRight className="size-6" aria-hidden="true" />
              </button>
            ) : null}
          </div>

          {hasMultipleImages ? (
            <div className="mx-auto flex max-w-full gap-2 overflow-x-auto rounded-xl bg-white/10 p-2">
              {images.map((image, index) => (
                <button
                  key={image.id}
                  type="button"
                  onClick={() => goToIndex(index)}
                  className={cn(
                    "h-16 w-16 shrink-0 overflow-hidden rounded-lg border bg-slate-900 transition focus:outline-none focus-visible:ring-2 focus-visible:ring-white/80 sm:h-20 sm:w-20",
                    index === activeIndex ? "border-white ring-2 ring-white/50" : "border-white/20 hover:border-white/70"
                  )}
                  aria-current={index === activeIndex ? "true" : undefined}
                  aria-label={mergedLabels.thumbnail(index + 1)}
                >
                  <img src={image.src} alt="" className="h-full w-full object-contain" loading="lazy" />
                </button>
              ))}
            </div>
          ) : null}
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}

