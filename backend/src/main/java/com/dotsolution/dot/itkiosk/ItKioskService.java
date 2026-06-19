package com.dotsolution.dot.itkiosk;

import com.dotsolution.dot.common.EntityNotFoundException;
import com.dotsolution.dot.itkiosk.entity.ItTicket;
import com.dotsolution.dot.itkiosk.repository.ItTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ItKioskService {

    @Autowired
    private ItTicketRepository itTicketRepository;

    public ItTicket createTicket(ItTicket ticket) {
        ticket.setStatus("PENDING");
        return itTicketRepository.save(ticket);
    }

    public List<ItTicket> getTicketsByEmployee(UUID employeeId) {
        return itTicketRepository.findByEmployeeId(employeeId);
    }

    public List<ItTicket> getAllTickets() {
        return itTicketRepository.findAll();
    }

    public ItTicket updateTicketStatus(UUID ticketId, String status, String assignedTo, String comment) {
        ItTicket ticket = itTicketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("IT Ticket not found with id: " + ticketId));

        if (status != null && !status.trim().isEmpty()) {
            ticket.setStatus(status.toUpperCase());
        }
        if (assignedTo != null && !assignedTo.trim().isEmpty()) {
            ticket.setAssignedTo(assignedTo);
        }
        if (comment != null && !comment.trim().isEmpty()) {
            if (ticket.getComments() == null || ticket.getComments().trim().isEmpty()) {
                ticket.setComments(comment);
            } else {
                ticket.setComments(ticket.getComments() + "\n" + comment);
            }
        }
        return itTicketRepository.save(ticket);
    }
}
