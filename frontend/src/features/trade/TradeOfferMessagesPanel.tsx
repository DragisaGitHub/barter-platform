import { useState } from "react";
import { MessageSquare, Send } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import type { TradeOfferStatus } from "@/api/generated/types";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import {
    useSendTradeOfferMessage,
    useTradeOfferMessages,
} from "./useTradeOfferMessages";

interface TradeOfferMessagesPanelProps {
    tradeOfferUuid: string;
    status: TradeOfferStatus;
}

const MAX_MESSAGE_LENGTH = 2000;

function formatMessageTime(value?: string) {
    if (!value) {
        return "";
    }

    return new Date(value).toLocaleString();
}

export function TradeOfferMessagesPanel({
                                            tradeOfferUuid,
                                            status,
                                        }: TradeOfferMessagesPanelProps) {
    const { user } = useAuth();
    const [content, setContent] = useState("");

    const {
        data: messages = [],
        isLoading,
        isError,
    } = useTradeOfferMessages(tradeOfferUuid);

    const sendMessage = useSendTradeOfferMessage(tradeOfferUuid);

    const isPending = status === "PENDING";
    const normalizedContent = content.trim();
    const canSend =
        isPending &&
        normalizedContent.length > 0 &&
        normalizedContent.length <= MAX_MESSAGE_LENGTH &&
        !sendMessage.isPending;

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();

        if (!canSend) {
            return;
        }

        await sendMessage.mutateAsync({ content: normalizedContent });
        setContent("");
    };

    return (
        <Card className="mt-6">
            <div className="flex items-center gap-2 mb-4">
                <MessageSquare className="size-5 text-indigo-500" />
                <div>
                    <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                        Messages
                    </h2>
                    <p className="text-sm text-slate-500 dark:text-slate-400">
                        Discuss this trade offer with the other participant.
                    </p>
                </div>
            </div>

            {!isPending && (
                <div className="mb-4 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300">
                    This trade offer is closed. Messages are read-only.
                </div>
            )}

            <div className="mb-4 max-h-[420px] overflow-y-auto rounded-xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-900/40">
                {isLoading ? (
                    <div className="flex justify-center py-10">
                        <Spinner />
                    </div>
                ) : isError ? (
                    <EmptyState
                        title="Messages could not be loaded"
                        description="Please try again later."
                    />
                ) : messages.length === 0 ? (
                    <EmptyState
                        title="No messages yet"
                        description="Start the conversation about this trade offer."
                    />
                ) : (
                    <div className="space-y-3">
                        {messages.map((message) => {
                            const isMine = message.senderUserUuid === user?.uuid;

                            return (
                                <div
                                    key={message.uuid}
                                    className={`flex ${isMine ? "justify-end" : "justify-start"}`}
                                >
                                    <div
                                        className={`max-w-[85%] rounded-2xl px-4 py-3 shadow-sm sm:max-w-[70%] ${
                                            isMine
                                                ? "bg-indigo-600 text-white"
                                                : "bg-white text-slate-900 dark:bg-slate-800 dark:text-slate-100"
                                        }`}
                                    >
                                        <div
                                            className={`mb-1 text-xs font-medium ${
                                                isMine
                                                    ? "text-indigo-100"
                                                    : "text-slate-500 dark:text-slate-400"
                                            }`}
                                        >
                                            {isMine ? "You" : message.senderUsername}
                                        </div>
                                        <p className="whitespace-pre-wrap break-words text-sm">
                                            {message.content}
                                        </p>
                                        <div
                                            className={`mt-2 text-right text-[11px] ${
                                                isMine
                                                    ? "text-indigo-100"
                                                    : "text-slate-400 dark:text-slate-500"
                                            }`}
                                        >
                                            {formatMessageTime(message.createdAt)}
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>

            <form onSubmit={handleSubmit} className="space-y-2">
        <textarea
            value={content}
            onChange={(event) => setContent(event.target.value)}
            disabled={!isPending || sendMessage.isPending}
            maxLength={MAX_MESSAGE_LENGTH}
            rows={3}
            placeholder={
                isPending
                    ? "Write a message..."
                    : "Messaging is disabled for closed offers."
            }
            className="w-full resize-none rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500 dark:disabled:bg-slate-800"
        />

                <div className="flex items-center justify-between gap-3">
                    <p
                        className={`text-xs ${
                            normalizedContent.length > MAX_MESSAGE_LENGTH
                                ? "text-red-500"
                                : "text-slate-500 dark:text-slate-400"
                        }`}
                    >
                        {normalizedContent.length}/{MAX_MESSAGE_LENGTH}
                    </p>

                    <Button type="submit" disabled={!canSend} isLoading={sendMessage.isPending}>
                        <Send className="mr-2 size-4" />
                        Send
                    </Button>
                </div>
            </form>
        </Card>
    );
}