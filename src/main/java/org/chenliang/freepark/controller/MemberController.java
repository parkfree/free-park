package org.chenliang.freepark.controller;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.Member;
import org.chenliang.freepark.model.MemberDto;
import org.chenliang.freepark.repository.MemberRepository;
import org.chenliang.freepark.repository.TenantRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    private TenantRepository tenantRepository;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping("/members/{id}")
    public MemberDto getMember(@PathVariable Integer id) {
        Member member = memberRepository.getOne(id);
        return modelMapper.map(member, MemberDto.class);
    }

    @GetMapping("/tenants/{id}/members")
    public List<MemberDto> getMembersOfTenant(@PathVariable Integer id) {
        return memberRepository.findByTenantId(id).stream()
                .map(member -> modelMapper.map(member, MemberDto.class))
                .collect(Collectors.toList());
    }

    @PostMapping("/members")
    public MemberDto createMember(@RequestBody MemberDto memberDto) {
        Member member = modelMapper.map(memberDto, Member.class);
        return modelMapper.map(memberRepository.save(member), MemberDto.class);
    }

    @PutMapping("/members/{id}")
    public MemberDto updateMember(@PathVariable Integer id, @RequestBody MemberDto memberDto) {
        Member member = memberRepository.getOne(id);
        member.setMemType(memberDto.getMemType());
        member.setMobile(memberDto.getMobile());
        member.setOpenId(memberDto.getOpenId());
        member.setUserId(memberDto.getUserId());
        member.setLastPaidAt(memberDto.getLastPaidAt());
        member.setTenant(tenantRepository.getOne(memberDto.getTenantId()));
        return modelMapper.map(memberRepository.save(member), MemberDto.class);
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
