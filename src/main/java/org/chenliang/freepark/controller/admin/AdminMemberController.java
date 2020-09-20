package org.chenliang.freepark.controller.admin;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.MemberRequest;
import org.chenliang.freepark.model.MemberResponse;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.MemberRepository;
import org.chenliang.freepark.repository.TenantRepository;
import org.chenliang.freepark.service.MemberService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminMemberController {
  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private MemberService memberService;

  @Autowired
  private ModelMapper modelMapper;

  @GetMapping("/members/{id}")
  public MemberResponse getMember(@PathVariable Integer id) {
    return memberRepository.findById(id)
        .map(member -> modelMapper.map(member, MemberResponse.class))
        .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
  }

  @GetMapping("/tenants/{id}/members")
  public List<MemberResponse> getMembersOfTenant(@PathVariable Integer id) {
    return memberRepository.findByTenantId(id).stream()
        .map(member -> modelMapper.map(member, MemberResponse.class))
        .collect(Collectors.toList());
  }

  @PostMapping("/tenants/{id}/members")
  public MemberResponse createMember(@PathVariable Integer id, @RequestBody MemberRequest request) {
    Tenant tenant = tenantRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    return memberService.createMember(request, tenant);
  }

  @PutMapping("/tenants/{tenantId}/members/{memberId}")
  public MemberResponse updateMember(@PathVariable Integer tenantId, @PathVariable Integer memberId,
                                     @RequestBody MemberRequest request) {
    Tenant tenant = tenantRepository.findById(tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    return memberService.updateMember(memberId, request, tenant);
  }

  @DeleteMapping("/members/{id}")
  public ResponseEntity<Void> deleteMember(@PathVariable Integer id) {
    if (!memberRepository.existsById(id)) {
      throw new ResourceNotFoundException("Member not found");
    }
    memberRepository.deleteById(id);
    return ResponseEntity.ok().build();
  }
}
