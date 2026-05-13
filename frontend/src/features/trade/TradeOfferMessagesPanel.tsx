import {
    useEffect,
    useRef,
    useState,
    type FormEvent,
    type KeyboardEvent,
} from "react";
import { MessageSquare, Radio, Send } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import type { TradeOfferStatus } from "@/api/generated/types";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { cn } from "@/utils";
import {
    type TradeOfferMessageListItem,
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

    const date = new Date(value);
    const now = new Date();
    const isSameDay = date.toDateString() === now.toDateString();

    return isSameDay
        ? date.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })
        : date.toLocaleString([], {
              month: "short",
              day: "numeric",
              hour: "numeric",
              minute: "2-digit",
          });
}

function getInitials(name?: string) {
    if (!name) {
        return "?";
    }

    return name
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0]?.toUpperCase() ?? "")
        .join("") || "?";
}

function buildMessageIdentity(
    message: TradeOfferMessageListItem,
    currentUserUuid?: string,
) {
    const isMine = message.senderUserUuid === currentUserUuid;

    return {
        isMine,
        displayName: isMine ? "You" : message.senderUsername,
        initials: getInitials(isMine ? "You" : message.senderUsername),
    };
}

export function TradeOfferMessagesPanel({
    tradeOfferUuid,
    status,
}: TradeOfferMessagesPanelProps) {
    const { user } = useAuth();
    const [content, setContent] = useState("");
    const [showNewMessages, setShowNewMessages] = useState(false);
    const [queuedMessageCount, setQueuedMessageCount] = useState(0);
    const viewportRef = useRef<HTMLDivElement | null>(null);
    const hasAutoScrolledInitiallyRef = useRef(false);
    const shouldScrollOnNextMessageRef = useRef(false);
    const isNearBottomRef = useRef(true);
    const previousLastMessageRef = useRef<string | null>(null);
    const previousMessageCountRef = useRef(0);

    const {
        data: messages = [],
        isLoading,
        isError,
        isFetching,
    } = useTradeOfferMessages(tradeOfferUuid);

    const sendMessage = useSendTradeOfferMessage(tradeOfferUuid, {
        uuid: user?.uuid,
        username: user?.username,
    });

    const isPending = status === "PENDING";
    const normalizedContent = content.trim();
    const currentLength = content.length;
    const isNearLimit = currentLength >= MAX_MESSAGE_LENGTH * 0.9;
    const isInvalidLength = currentLength >= MAX_MESSAGE_LENGTH;
    const canSend =
        isPending &&
        normalizedContent.length > 0 &&
        normalizedContent.length <= MAX_MESSAGE_LENGTH &&
        !sendMessage.isPending;

    const scrollToBottom = (behavior: ScrollBehavior = "smooth") => {
        const viewport = viewportRef.current;

        if (!viewport) {
            return;
        }

        viewport.scrollTo({
            top: viewport.scrollHeight,
            behavior,
        });

        isNearBottomRef.current = true;
        setShowNewMessages(false);
        setQueuedMessageCount(0);
    };

    const updateNearBottomState = () => {
        const viewport = viewportRef.current;

        if (!viewport) {
            return;
        }

        const distanceFromBottom =
            viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight;
        const nearBottom = distanceFromBottom <= 96;

        isNearBottomRef.current = nearBottom;

        if (nearBottom) {
            setShowNewMessages(false);
            setQueuedMessageCount(0);
        }
    };

    useEffect(() => {
        updateNearBottomState();
    }, []);

    useEffect(() => {
        const lastMessage = messages[messages.length - 1];
        const lastMessageKey = lastMessage
            ? `${lastMessage.uuid}:${lastMessage.createdAt}:${lastMessage.isOptimistic ? "optimistic" : "confirmed"}`
            : null;

        if (!lastMessageKey) {
            previousLastMessageRef.current = null;
            previousMessageCountRef.current = 0;
            hasAutoScrolledInitiallyRef.current = false;
            setShowNewMessages(false);
            setQueuedMessageCount(0);
            return;
        }

        if (!hasAutoScrolledInitiallyRef.current) {
            scrollToBottom("auto");
            hasAutoScrolledInitiallyRef.current = true;
        } else if (lastMessageKey !== previousLastMessageRef.current) {
            const newMessagesDelta = Math.max(
                messages.length - previousMessageCountRef.current,
                1,
            );

            if (shouldScrollOnNextMessageRef.current) {
                scrollToBottom("smooth");
                shouldScrollOnNextMessageRef.current = false;
            } else if (isNearBottomRef.current) {
                scrollToBottom("smooth");
            } else {
                setShowNewMessages(true);
                setQueuedMessageCount((currentCount) => currentCount + newMessagesDelta);
            }
        }

        previousLastMessageRef.current = lastMessageKey;
        previousMessageCountRef.current = messages.length;
    }, [messages]);

    const submitMessage = async () => {
        if (!canSend) {
            return;
        }

        shouldScrollOnNextMessageRef.current = true;

        try {
            await sendMessage.mutateAsync({ content: normalizedContent });
            setContent("");
        } catch {
            shouldScrollOnNextMessageRef.current = false;
        }
    };

    const handleSubmit = async (event: FormEvent) => {
        event.preventDefault();
        await submitMessage();
    };

    const handleComposerKeyDown = async (
        event: KeyboardEvent<HTMLTextAreaElement>,
    ) => {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            await submitMessage();
        }
    };

    const messageCount = messages.length;
    const pollIndicatorLabel = isFetching ? "Syncing updates" : "Polling every 15s";
    const counterClassName = isInvalidLength
        ? "text-red-500 dark:text-red-400"
        : isNearLimit
          ? "text-amber-600 dark:text-amber-400"
          : "text-slate-500 dark:text-slate-400";

    return (
        <Card className="mt-6 overflow-hidden border-slate-200/80 bg-white/95 p-0 shadow-sm dark:border-slate-700/80 dark:bg-slate-900/95">
            <div className="sticky top-0 z-10 border-b border-slate-200/80 bg-white/90 backdrop-blur dark:border-slate-700/80 dark:bg-slate-900/90">
                <div className="flex flex-col gap-3 px-4 py-4 sm:px-5">
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                        <div className="flex min-w-0 items-start gap-3">
                            <div className="flex size-10 shrink-0 items-center justify-center rounded-2xl border border-indigo-200 bg-indigo-50 text-indigo-600 shadow-sm dark:border-indigo-900/60 dark:bg-indigo-950/50 dark:text-indigo-300">
                                <MessageSquare className="size-5" />
                            </div>
                            <div className="min-w-0">
                                <div className="flex flex-wrap items-center gap-2">
                                    <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100 sm:text-lg">
                                        Trade conversation
                                    </h2>
                                    <Badge variant={isPending ? "success" : "default"}>
                                        {isPending ? "Active conversation" : "Conversation closed"}
                                    </Badge>
                                </div>
                                <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                                    Keep the negotiation here so both traders can confirm condition,
                                    timing, pickup, delivery, or exchange details in one thread.
                                </p>
                            </div>
                        </div>

                        <div className="flex flex-wrap items-center gap-2 sm:justify-end">
                            <div className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600 dark:border-slate-700 dark:bg-slate-800/80 dark:text-slate-300">
                                <Radio
                                    className={cn(
                                        "size-3.5",
                                        isFetching
                                            ? "animate-pulse text-emerald-500"
                                            : "text-slate-400 dark:text-slate-500",
                                    )}
                                />
                                <span>{pollIndicatorLabel}</span>
                            </div>

                            <div className="inline-flex items-center rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600 dark:border-slate-700 dark:bg-slate-800/80 dark:text-slate-300">
                                {messageCount} {messageCount === 1 ? "message" : "messages"}
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="relative border-b border-slate-200/80 bg-slate-50/70 dark:border-slate-700/80 dark:bg-slate-950/30">
                <div
                    ref={viewportRef}
                    role="log"
                    aria-live="polite"
                    aria-busy={isLoading}
                    onScroll={updateNearBottomState}
                    className="max-h-[min(65vh,34rem)] overflow-y-auto overflow-x-hidden px-3 py-3 sm:px-4 sm:py-4"
                >
                    {isLoading && messages.length === 0 ? (
                        <div className="flex justify-center py-12">
                            <Spinner />
                        </div>
                    ) : isError ? (
                        <EmptyState
                            title="Messages could not be loaded"
                            description="Please try again in a moment."
                        />
                    ) : messages.length === 0 ? (
                        <div className="flex min-h-[260px] items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white/80 px-6 py-10 text-center dark:border-slate-700 dark:bg-slate-900/40">
                            <EmptyState
                                title="Start the trade conversation"
                                description="Ask about condition, exchange details, availability, pickup, delivery, or shipping before you respond."
                            />
                        </div>
                    ) : (
                        <div className="space-y-2.5">
                            {messages.map((message) => {
                                const { isMine, displayName, initials } = buildMessageIdentity(
                                    message,
                                    user?.uuid,
                                );

                                return (
                                    <div
                                        key={message.uuid}
                                        className={cn(
                                            "flex items-end gap-2 sm:gap-3",
                                            isMine ? "justify-end" : "justify-start",
                                        )}
                                    >
                                        {!isMine && (
                                            <div className="flex size-8 shrink-0 items-center justify-center rounded-full border border-slate-200 bg-white text-[11px] font-semibold text-slate-600 shadow-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300">
                                                {initials}
                                            </div>
                                        )}

                                        <div
                                            className={cn(
                                                "flex min-w-0 max-w-[92%] flex-col sm:max-w-[76%]",
                                                isMine ? "items-end" : "items-start",
                                            )}
                                        >
                                            <div
                                                className={cn(
                                                    "mb-1 flex items-center gap-2 px-1 text-[11px] font-medium text-slate-500 dark:text-slate-400",
                                                    isMine && "justify-end",
                                                )}
                                            >
                                                <span>{displayName}</span>
                                                {message.isOptimistic && (
                                                    <span className="rounded-full bg-indigo-50 px-2 py-0.5 text-[10px] font-semibold text-indigo-600 dark:bg-indigo-950/50 dark:text-indigo-300">
                                                        Sending…
                                                    </span>
                                                )}
                                            </div>

                                            <div
                                                className={cn(
                                                    "group/bubble w-fit max-w-full rounded-2xl border px-3.5 py-2.5 text-sm leading-5 shadow-sm transition-all duration-150 sm:px-4",
                                                    isMine
                                                        ? "border-indigo-500/70 bg-indigo-600 text-white shadow-indigo-950/10 hover:border-indigo-400 hover:bg-indigo-600/95"
                                                        : "border-slate-200 bg-white text-slate-900 hover:border-slate-300 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100 dark:hover:border-slate-600 dark:hover:bg-slate-800/90",
                                                    message.isOptimistic &&
                                                        "opacity-90 ring-1 ring-indigo-200/80 dark:ring-indigo-900/70",
                                                )}
                                            >
                                                <p className="whitespace-pre-wrap [overflow-wrap:anywhere]">
                                                    {message.content}
                                                </p>

                                                <div
                                                    className={cn(
                                                        "mt-2 text-[11px]",
                                                        isMine
                                                            ? "text-right text-indigo-100"
                                                            : "text-slate-400 dark:text-slate-500",
                                                    )}
                                                >
                                                    {message.isOptimistic
                                                        ? "Sending…"
                                                        : formatMessageTime(message.createdAt)}
                                                </div>
                                            </div>
                                        </div>

                                        {isMine && (
                                            <div className="flex size-8 shrink-0 items-center justify-center rounded-full border border-indigo-200 bg-indigo-50 text-[11px] font-semibold text-indigo-700 shadow-sm dark:border-indigo-900/60 dark:bg-indigo-950/50 dark:text-indigo-300">
                                                {initials}
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                {showNewMessages && (
                    <div className="pointer-events-none absolute inset-x-0 bottom-4 flex justify-center px-4">
                        <Button
                            type="button"
                            size="sm"
                            className="pointer-events-auto rounded-full border border-indigo-200 bg-white/95 px-4 shadow-lg shadow-slate-900/5 backdrop-blur hover:bg-white dark:border-indigo-900/60 dark:bg-slate-900/95 dark:hover:bg-slate-900"
                            onClick={() => scrollToBottom("smooth")}
                        >
                            New {queuedMessageCount > 1 ? "messages" : "message"}
                        </Button>
                    </div>
                )}
            </div>

            <div
                className={cn(
                    "sticky bottom-0 bg-white/95 px-4 py-4 backdrop-blur dark:bg-slate-900/95 sm:px-5",
                    !isPending && "opacity-70",
                )}
            >
                <form onSubmit={handleSubmit} className="space-y-3">
                    <div
                        className={cn(
                            "overflow-hidden rounded-2xl border border-slate-200 bg-slate-50 shadow-sm transition-colors dark:border-slate-700 dark:bg-slate-950/40",
                            isPending &&
                                "focus-within:border-indigo-400 focus-within:bg-white focus-within:ring-4 focus-within:ring-indigo-500/10 dark:focus-within:border-indigo-500 dark:focus-within:bg-slate-900",
                        )}
                    >
                        <textarea
                            value={content}
                            onChange={(event) => setContent(event.target.value)}
                            onKeyDown={handleComposerKeyDown}
                            disabled={!isPending || sendMessage.isPending}
                            maxLength={MAX_MESSAGE_LENGTH}
                            rows={3}
                            placeholder={
                                isPending
                                    ? "Ask about condition, availability, pickup, delivery, or the swap details…"
                                    : "This conversation is closed. Messages remain visible for reference."
                            }
                            className="block min-h-[96px] w-full resize-none border-0 bg-transparent px-4 py-3 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none disabled:cursor-not-allowed disabled:text-slate-500 dark:text-slate-100 dark:placeholder:text-slate-500 dark:disabled:text-slate-400"
                        />
                    </div>

                    <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                        <div className="space-y-1">
                            <p className="text-xs text-slate-500 dark:text-slate-400">
                                Keep communication inside Barter Platform for safer trading.
                            </p>
                            <p className={cn("text-xs font-medium", counterClassName)}>
                                {currentLength}/{MAX_MESSAGE_LENGTH}
                            </p>
                        </div>

                        <Button
                            type="submit"
                            disabled={!canSend}
                            isLoading={sendMessage.isPending}
                            className="min-h-11 rounded-xl px-4 py-2.5"
                        >
                            <Send className="size-4" />
                            {sendMessage.isPending ? "Sending…" : "Send message"}
                        </Button>
                    </div>

                    {!isPending && (
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                            This trade offer is closed, so the conversation is read-only.
                        </p>
                    )}
                </form>
            </div>
        </Card>
    );
}