package com.easyfamily.finance.service;

/**
 * Immutable value object representing a user's role in the family finance system.
 *
 * <ul>
 *   <li>{@code role} — one of {@code "head"}, {@code "viewer"}, {@code "none"}</li>
 *   <li>{@code headUserId} — {@code null} when the user is the head; set to the
 *       head's {@code user_id} when the user is a viewer</li>
 * </ul>
 */
public record FinanceRole(String role, String headUserId) {

    public boolean isHead() {
        return "head".equals(role);
    }

    public boolean isViewer() {
        return "viewer".equals(role);
    }

    public boolean hasAccess() {
        return isHead() || isViewer();
    }

    /**
     * Returns the {@code userId} whose data should actually be read/written.
     * For a head this is {@code selfUserId}; for a viewer it is the head's id.
     */
    public String dataUserId(String selfUserId) {
        return isViewer() ? headUserId : selfUserId;
    }
}
