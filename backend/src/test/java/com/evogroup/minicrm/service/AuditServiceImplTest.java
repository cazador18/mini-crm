package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.AuditLogResponse;
import com.evogroup.minicrm.dto.PageResponse;
import com.evogroup.minicrm.model.AuditLog;
import com.evogroup.minicrm.model.User;
import com.evogroup.minicrm.repository.AuditLogRepository;
import com.evogroup.minicrm.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditLogRepository repository;

    @Mock
    private CurrentUserService currentUserService;

    private AuditServiceImpl service;

    private User manager;

    @BeforeEach
    void setUp() {
        service = new AuditServiceImpl(repository, currentUserService);

        manager = new User();
        manager.setId(1L);
        manager.setUsername("alice");
    }

    @Test
    void log_savesEntryWithCurrentUserAndGivenFields() {
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(repository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        service.log("CREATE", "CLIENT", 42L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getWho()).isEqualTo("alice");
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getEntity()).isEqualTo("CLIENT");
        assertThat(saved.getEntityId()).isEqualTo(42L);
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void findAll_mapsToPageResponse() {
        AuditLog entry = new AuditLog();
        entry.setId(1L);
        entry.setWho("alice");
        entry.setAction("CREATE");
        entry.setEntity("CLIENT");
        entry.setEntityId(42L);
        entry.setTimestamp(Instant.parse("2026-01-01T00:00:00Z"));

        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findAllByOrderByTimestampDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(entry), pageable, 1));

        PageResponse<AuditLogResponse> result = service.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        AuditLogResponse response = result.getContent().get(0);
        assertThat(response.getWho()).isEqualTo("alice");
        assertThat(response.getAction()).isEqualTo("CREATE");
        assertThat(response.getEntity()).isEqualTo("CLIENT");
        assertThat(response.getEntityId()).isEqualTo(42L);
    }
}
