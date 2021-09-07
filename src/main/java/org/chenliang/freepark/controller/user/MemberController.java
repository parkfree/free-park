package org.chenliang.freepark.controller.user;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.MemberRequest;
import org.chenliang.freepark.model.MemberResponse;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.model.rtmap.RtmapMember;
import org.chenliang.freepark.repository.MemberRepository;
import org.chenliang.freepark.service.MemberService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class MemberController {
  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private MemberService memberService;

  @Autowired
  private ModelMapper modelMapper;

  @GetMapping("/members/{id}")
  public MemberResponse getMember(@PathVariable Integer id, @AuthenticationPrincipal Tenant tenant) {
    return memberRepository.findFirstByIdAndTenantId(id, tenant.getId())
                           .map(member -> modelMapper.map(member, MemberResponse.class))
                           .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
  }

  @GetMapping("/members")
  public List<MemberResponse> getMembers(@AuthenticationPrincipal Tenant tenant) {
    return memberRepository.findByTenantId(tenant.getId()).stream()
                           .map(member -> modelMapper.map(member, MemberResponse.class))
                           .collect(Collectors.toList());
  }

  @PostMapping("/members")
  public MemberResponse createMember(@RequestBody @Validated MemberRequest request, @AuthenticationPrincipal Tenant tenant) {
    return modelMapper.map(memberService.createMember(request, tenant), MemberResponse.class);
  }

  @PutMapping("/members/{id}")
  public MemberResponse updateMember(@PathVariable Integer id, @RequestBody @Validated MemberRequest request,
                                     @AuthenticationPrincipal Tenant tenant) {
    return modelMapper.map(memberService.updateMember(id, request, tenant), MemberResponse.class);
  }

  @DeleteMapping("/members/{id}")
  public ResponseEntity<Void> deleteMember(@PathVariable Integer id, @AuthenticationPrincipal Tenant tenant) {
    if (!memberRepository.existsByIdAndTenantId(id, tenant.getId())) {
      throw new ResourceNotFoundException("Member not found");
    }
    memberRepository.deleteById(id);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/rtmap-member/{mobile}")
  public RtmapMember getRtmapMember(@PathVariable String mobile) {
    return memberService.getRtmapMemberDetailByMobile(mobile);
  }
}
