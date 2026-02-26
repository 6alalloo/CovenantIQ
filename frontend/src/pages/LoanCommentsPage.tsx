import { FormEvent, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { addComment, deleteComment, getComments } from "../api/client";
import type { CommentResponse } from "../types/api";
import { Surface } from "../components/layout";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";

export function LoanCommentsPage() {
  const { loanId } = useParams();
  const numericLoanId = Number(loanId);
  const [comments, setComments] = useState<CommentResponse[]>([]);
  const [text, setText] = useState("");
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      const page = await getComments(numericLoanId);
      setComments(page.content);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    void load();
  }, [numericLoanId]);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!text.trim()) return;
    try {
      await addComment(numericLoanId, text.trim());
      setText("");
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const onDelete = async (commentId: number) => {
    try {
      await deleteComment(numericLoanId, commentId);
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <Surface className="p-4">
      <h2 className="panel-title">Collaboration Notes</h2>
      <form className="mt-3 flex gap-2" onSubmit={onSubmit}>
        <Input
          value={text}
          onChange={(event) => setText(event.target.value)}
          placeholder="Add a comment with context or handoff notes"
        />
        <Button type="submit">
          Post
        </Button>
      </form>

      <div className="mt-4 space-y-2">
        {comments.map((comment) => (
          <article key={comment.id} className="rounded-sm border border-[var(--border-default)] bg-[var(--bg-surface-2)] p-3">
            <div className="mb-2 flex items-start justify-between">
              <p className="text-xs text-[var(--text-secondary)]">
                {comment.createdBy} | {comment.createdAt}
              </p>
              <Button variant="ghost" className="h-auto px-0 py-0 text-xs text-[var(--risk-high)]" onClick={() => void onDelete(comment.id)} type="button">
                Delete
              </Button>
            </div>
            <p className="text-sm">{comment.commentText}</p>
          </article>
        ))}
      </div>
      {error ? <p className="mt-3 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </Surface>
  );
}
