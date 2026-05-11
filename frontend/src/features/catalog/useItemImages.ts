import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import type { ItemImageResponse } from "@/api/generated/types.ts";
import {
  uploadItemImage,
  listItemImages,
  deleteItemImage,
  setPrimaryItemImage,
} from "@/api/itemImageApi.ts";
import { catalogKeys } from "./useCatalog";

// ─── Query keys ─────────────────────────────────────────────────────────────

export const itemImageKeys = {
  list: (itemUuid: string) => ["catalog", "items", itemUuid, "images"] as const,
};

// ─── Query hooks ────────────────────────────────────────────────────────────

export function useItemImages(itemUuid: string) {
  return useQuery<ItemImageResponse[]>({
    queryKey: itemImageKeys.list(itemUuid),
    queryFn: () => listItemImages(itemUuid),
    enabled: !!itemUuid,
  });
}

// ─── Mutation hooks ─────────────────────────────────────────────────────────

export function useUploadItemImage(itemUuid: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ file, onProgress }: { file: File; onProgress?: (percent: number) => void }) =>
      uploadItemImage(itemUuid, file, onProgress),
    onSuccess: () => {
      toast.success("Image uploaded successfully");
      queryClient.invalidateQueries({ queryKey: itemImageKeys.list(itemUuid) });
      queryClient.invalidateQueries({ queryKey: catalogKeys.itemDetail(itemUuid) });
      queryClient.invalidateQueries({ queryKey: catalogKeys.items });
    },
    onError: () => {
      toast.error("Failed to upload image");
    },
  });
}

export function useDeleteItemImage(itemUuid: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (imageUuid: string) => deleteItemImage(itemUuid, imageUuid),
    onSuccess: () => {
      toast.success("Image deleted");
      queryClient.invalidateQueries({ queryKey: itemImageKeys.list(itemUuid) });
      queryClient.invalidateQueries({ queryKey: catalogKeys.itemDetail(itemUuid) });
      queryClient.invalidateQueries({ queryKey: catalogKeys.items });
    },
    onError: () => {
      toast.error("Failed to delete image");
    },
  });
}

export function useSetPrimaryItemImage(itemUuid: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (imageUuid: string) => setPrimaryItemImage(itemUuid, imageUuid),
    onSuccess: () => {
      toast.success("Primary image updated");
      queryClient.invalidateQueries({ queryKey: itemImageKeys.list(itemUuid) });
      queryClient.invalidateQueries({ queryKey: catalogKeys.itemDetail(itemUuid) });
      queryClient.invalidateQueries({ queryKey: catalogKeys.items });
    },
    onError: () => {
      toast.error("Failed to set primary image");
    },
  });
}

