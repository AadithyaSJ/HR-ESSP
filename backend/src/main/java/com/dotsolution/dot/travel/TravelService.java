package com.dotsolution.dot.travel;

import com.dotsolution.dot.common.EntityNotFoundException;
import com.dotsolution.dot.travel.entity.TravelRequest;
import com.dotsolution.dot.travel.repository.TravelRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TravelService {

    @Autowired
    private TravelRequestRepository travelRequestRepository;

    public TravelRequest createTravelRequest(TravelRequest request) {
        request.setStatus("PENDING");
        return travelRequestRepository.save(request);
    }

    public List<TravelRequest> getTravelRequestsByEmployee(UUID employeeId) {
        return travelRequestRepository.findByEmployeeId(employeeId);
    }

    public List<TravelRequest> getTravelRequestsByManager(UUID managerId) {
        return travelRequestRepository.findByManagerId(managerId);
    }

    public List<TravelRequest> getAllTravelRequests() {
        return travelRequestRepository.findAll();
    }

    public TravelRequest approveTravelRequest(UUID id) {
        TravelRequest request = travelRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Travel request not found with id: " + id));
        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Travel request must be PENDING to approve");
        }
        request.setStatus("APPROVED");
        return travelRequestRepository.save(request);
    }

    public TravelRequest rejectTravelRequest(UUID id, String rejectionReason) {
        TravelRequest request = travelRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Travel request not found with id: " + id));
        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Travel request must be PENDING to reject");
        }
        request.setStatus("REJECTED");
        request.setRejectionReason(rejectionReason);
        return travelRequestRepository.save(request);
    }

    public TravelRequest bookTravelRequest(UUID id) {
        TravelRequest request = travelRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Travel request not found with id: " + id));
        if (!"APPROVED".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Travel request must be APPROVED to mark as BOOKED");
        }
        request.setStatus("BOOKED");
        return travelRequestRepository.save(request);
    }
}
