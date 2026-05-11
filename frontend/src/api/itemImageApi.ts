import { apiClient } from "./axios";
import type { ItemImageResponse } from "./generated/types";

// ─── API functions ──────────────────────────────────────────────────────────

export async function uploadItemImage(
  itemUuid: string,
  file: File,
  onProgress?: (percent: number) => void
): Promise<ItemImageResponse> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await apiClient.post<ItemImageResponse>(
    `/catalog/items/${itemUuid}/images`,
    formData,
    {
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: onProgress
        ? (event) => {
            if (event.total) {
              onProgress(Math.round((event.loaded * 100) / event.total));
            }
          }
        : undefined,
    }
  );
  return response.data;
}

export async function listItemImages(itemUuid: string): Promise<ItemImageResponse[]> {
  const response = await apiClient.get<ItemImageResponse[]>(
    `/catalog/items/${itemUuid}/images`
  );
  return response.data;
}

export async function deleteItemImage(
  itemUuid: string,
  imageUuid: string
): Promise<void> {
  await apiClient.delete(`/catalog/items/${itemUuid}/images/${imageUuid}`);
}

export async function setPrimaryItemImage(
  itemUuid: string,
  imageUuid: string
): Promise<ItemImageResponse> {
  const response = await apiClient.put<ItemImageResponse>(
    `/catalog/items/${itemUuid}/images/${imageUuid}/primary`
  );
  return response.data;
}

