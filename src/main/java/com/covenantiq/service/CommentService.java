package com.covenantiq.service;

import com.covenantiq.domain.Comment;
import com.covenantiq.domain.Loan;
import com.covenantiq.dto.response.CommentResponse;
import com.covenantiq.enums.ActivityEventType;
import com.covenantiq.exception.ForbiddenOperationException;
import com.covenantiq.exception.ResourceNotFoundException;
import com.covenantiq.repository.CommentRepository;
import com.covenantiq.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final LoanService loanService;
    private final CurrentUserService currentUserService;
    private final ActivityLogService activityLogService;

    public CommentService(
            CommentRepository commentRepository,
            LoanService loanService,
            CurrentUserService currentUserService,
            ActivityLogService activityLogService
    ) {
        this.commentRepository = commentRepository;
        this.loanService = loanService;
        this.currentUserService = currentUserService;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public CommentResponse addComment(Long loanId, String text) {
        Loan loan = loanService.getLoan(loanId);
        Comment comment = new Comment();
        comment.setLoan(loan);
        comment.setCommentText(text.trim());
        comment.setCreatedBy(currentUserService.usernameOrSystem());
        comment.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        Comment saved = commentRepository.save(comment);
        activityLogService.logEvent(
                ActivityEventType.COMMENT_ADDED,
                "Comment",
                saved.getId(),
                loanId,
                "Comment added to loan " + loanId
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(Long loanId, Pageable pageable) {
        loanService.getLoan(loanId);
        Pageable withSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return commentRepository.findByLoanId(loanId, withSort).map(this::toResponse);
    }

    @Transactional
    public void deleteComment(Long loanId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment " + commentId + " not found"));
        if (!comment.getLoan().getId().equals(loanId)) {
            throw new ResourceNotFoundException("Comment " + commentId + " not found for loan " + loanId);
        }
        String current = currentUserService.usernameOrSystem();
        if (!current.equals(comment.getCreatedBy()) && !currentUserService.hasRole("ADMIN")) {
            throw new ForbiddenOperationException("Only comment creator or ADMIN can delete comments");
        }
        commentRepository.delete(comment);
        activityLogService.logEvent(
                ActivityEventType.COMMENT_DELETED,
                "Comment",
                commentId,
                loanId,
                "Comment deleted from loan " + loanId
        );
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getLoan().getId(),
                comment.getCommentText(),
                comment.getCreatedBy(),
                comment.getCreatedAt()
        );
    }
}
