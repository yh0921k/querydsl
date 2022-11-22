package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QueryDSLBasicTest {

  @Autowired EntityManager em;
  JPAQueryFactory queryFactory;


  @BeforeEach
  public void beforeEach() {
    queryFactory = new JPAQueryFactory(em);

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

    em.flush();
    em.clear();
  }

  @Test
  public void startJPQL() {
    // find member1
    String query = "select m from Member m where m.username = :username";
    Member findMember =
        em.createQuery(query, Member.class).setParameter("username", "member1").getSingleResult();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void startQuerydsl() {
    // QMember m = new QMember("m");
    QMember qMember = QMember.member;

    Member findMember =
        queryFactory.select(qMember).from(qMember).where(qMember.username.eq("member1")).fetchOne();
    assertThat(findMember.getUsername()).isEqualTo("member1");
  }
}
