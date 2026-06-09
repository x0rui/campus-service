package com.campus.service.service;

import com.campus.service.entity.ClubApplication;
import com.campus.service.mapper.ClubApplicationMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClubApplicationService {

    private final ClubApplicationMapper clubApplicationMapper;

    public ClubApplicationService(ClubApplicationMapper clubApplicationMapper) {
        this.clubApplicationMapper = clubApplicationMapper;
    }

    public ClubApplication apply(Long userId, String clubName, String description) {
        ClubApplication ca = new ClubApplication();
        ca.setUserId(userId);
        ca.setClubName(clubName);
        ca.setDescription(description);
        ca.setStatus(0);
        clubApplicationMapper.insert(ca);
        return ca;
    }

    public List<ClubApplication> getPending() {
        return clubApplicationMapper.selectPendingList();
    }

    public List<ClubApplication> getMy(Long userId) {
        return clubApplicationMapper.selectByUserId(userId);
    }
}
