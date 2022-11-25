package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

  @Autowired
  EntityManager em;

  @Autowired MemberJpaRepository memberJpaRepository;

  @Test
  public void basicTest() {
    Member member = new Member("member1", 10);
    memberJpaRepository.save(member);

    Member findMember = memberJpaRepository.findById(member.getId()).get();
    assertThat(findMember).isEqualTo(member);

    List<Member> result1 = memberJpaRepository.findAll();
    assertThat(result1).contains(member);

    List<Member> result2 = memberJpaRepository.findByUsername("member1");
    assertThat(result2).containsExactly(member);
  }

  @Test
  public void basicQuerydslTest() {
    Member member = new Member("member1", 10);
    memberJpaRepository.save(member);

    List<Member> result1 = memberJpaRepository.findAllQuerydsl();
    assertThat(result1).contains(member);

    List<Member> result2 = memberJpaRepository.findByUsernameQuerydsl("member1");
    assertThat(result2).containsExactly(member);
  }

  @Test
  public void searchTest() {
    Team teamA = new Team("TeamA");
    Team teamB = new Team("TeamB");
    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);
    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);
    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);

    MemberSearchCondition memberSearchCondition = new MemberSearchCondition();
    memberSearchCondition.setAgeGoe(35);
    memberSearchCondition.setAgeLoe(40);
    memberSearchCondition.setTeamName("TeamB");

    List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(memberSearchCondition);

    assertThat(result).extracting("username").containsExactly("member4");
  }
}