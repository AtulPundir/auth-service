package com.myapp.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cleanup Response DTO
 * Matches Node.js cleanup endpoints return type
 *
 * TypeScript interface:
 * { deleted: number }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleanupResponse {

    private Integer deleted; // Number of deleted records
}
