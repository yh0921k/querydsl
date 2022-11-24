package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

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
    Member findMember =
        queryFactory.select(member).from(member).where(member.username.eq("member1")).fetchOne();
    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void search() {
    Member findMember =
        queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1").and(member.age.eq(10)))
            .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void searchAndParam() {
    Member findMember =
        queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1"), member.age.eq(10))
            .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void resultFetch() {
    List<Member> fetch = queryFactory.selectFrom(member).fetch();

    Member fetchOne = queryFactory.selectFrom(QMember.member).fetchOne();

    Member fetchFirst = queryFactory.selectFrom(QMember.member).fetchFirst();

    QueryResults<Member> fetchResults = queryFactory.selectFrom(member).fetchResults();
    fetchResults.getTotal();
    List<Member> fetchResultsMembers = fetchResults.getResults();

    long count = queryFactory.selectFrom(member).fetchCount();
  }

  @Test
  public void sort() {
    em.persist(new Member(null, 100));
    em.persist(new Member("member5", 100));
    em.persist(new Member("member6", 100));

    List<Member> result =
        queryFactory
            .selectFrom(member)
            .where(member.age.eq(100))
            .orderBy(member.age.desc(), member.username.asc().nullsLast())
            .fetch();

    Member member5 = result.get(0);
    Member member6 = result.get(1);
    Member memberNull = result.get(2);

    assertThat(member5.getUsername()).isEqualTo("member5");
    assertThat(member6.getUsername()).isEqualTo("member6");
    assertThat(memberNull.getUsername()).isNull();
  }

  @Test
  public void paging() {
    List<Member> result =
        queryFactory.selectFrom(member).orderBy(member.username.desc()).offset(1).limit(2).fetch();
    assertThat(result.size()).isEqualTo(2);
  }

  @Test
  public void pagingWithTotal() {
    QueryResults<Member> queryResults =
        queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetchResults();

    assertThat(queryResults.getTotal()).isEqualTo(4);
    assertThat(queryResults.getLimit()).isEqualTo(2);
    assertThat(queryResults.getOffset()).isEqualTo(1);
    assertThat(queryResults.getResults().size()).isEqualTo(2);
  }

  @Test
  public void aggregation() {
    List<Tuple> result =
        queryFactory
            .select(
                member.count(),
                member.age.sum(),
                member.age.avg(),
                member.age.max(),
                member.age.min())
            .from(member)
            .fetch();

    Tuple tuple = result.get(0);
    assertThat(tuple.get(member.count())).isEqualTo(4);
    assertThat(tuple.get(member.age.sum())).isEqualTo(100);
    assertThat(tuple.get(member.age.avg())).isEqualTo(25);
    assertThat(tuple.get(member.age.max())).isEqualTo(40);
    assertThat(tuple.get(member.age.min())).isEqualTo(10);
  }

  @Test
  public void groupBy() {
    List<Tuple> result =
        queryFactory
            .select(team.name, member.age.avg())
            .from(member)
            .join(member.team, team)
            .groupBy(team.name)
            .fetch();

    Tuple teamA = result.get(0);
    Tuple teamB = result.get(1);

    assertThat(teamA.get(team.name)).isEqualTo("TeamA");
    assertThat(teamA.get(member.age.avg())).isEqualTo(15);

    assertThat(teamB.get(team.name)).isEqualTo("TeamB");
    assertThat(teamB.get(member.age.avg())).isEqualTo(35);
  }

  @Test
  public void join() {
    List<Member> result =
        queryFactory
            .selectFrom(member)
            .join(member.team, team)
            .where(team.name.eq("TeamA"))
            .fetch();
    assertThat(result).extracting("username").containsExactly("member1", "member2");
  }

  @Test
  public void thetaJoin() {
    em.persist(new Member("TeamA"));
    em.persist(new Member("TeamB"));
    em.persist(new Member("TeamC"));

    List<Member> result =
        queryFactory.select(member).from(member, team).where(member.username.eq(team.name)).fetch();
    assertThat(result).extracting("username").containsExactly("TeamA", "TeamB");
  }

  @Test
  public void joinOnFiltering() {
    List<Tuple> result =
        queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(member.team, team)
            .on(team.name.eq("TeamA"))
            .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  @Test
  public void joinOnNoRelation() {
    em.persist(new Member("TeamA"));
    em.persist(new Member("TeamB"));
    em.persist(new Member("TeamC"));

    List<Tuple> result =
        queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(team)
            .on(member.username.eq(team.name))
            .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  @PersistenceUnit EntityManagerFactory emf;

  @Test
  public void noFetchJoin() {
    em.flush();
    em.clear();

    Member findMember =
        queryFactory.selectFrom(member).where(member.username.eq("member1")).fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).as("페치 조인 미적용").isFalse();
  }

  @Test
  public void fetchJoin() {
    em.flush();
    em.clear();

    Member findMember =
        queryFactory
            .selectFrom(member)
            .join(member.team, team)
            .fetchJoin()
            .where(member.username.eq("member1"))
            .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).as("페치 조인 적용").isTrue();
  }

  @Test
  public void subQuery() {
    // 나이가 가장 많은 회원을 조회
    QMember memberSub = new QMember("memberSub");
    List<Member> result =
        queryFactory
            .selectFrom(member)
            .where(member.age.eq(select(memberSub.age.max()).from(memberSub)))
            .fetch();

    assertThat(result).extracting("age").containsExactly(40);
  }

  @Test
  public void subQueryGoe() {
    // 나이가 평균 이상인 회원
    QMember memberSub = new QMember("memberSub");
    List<Member> result =
        queryFactory
            .selectFrom(member)
            .where(member.age.goe(select(memberSub.age.avg()).from(memberSub)))
            .fetch();

    assertThat(result).extracting("age").containsExactly(30, 40);
  }

  @Test
  public void subQueryIn() {
    // 나이가 평균 이상인 회원
    QMember memberSub = new QMember("memberSub");
    List<Member> result =
        queryFactory
            .selectFrom(member)
            .where(member.age.in(select(memberSub.age).from(memberSub).where(memberSub.age.gt(10))))
            .fetch();

    assertThat(result).extracting("age").containsExactly(20, 30, 40);
  }

  @Test
  public void selectSubQuery() {
    QMember memberSub = new QMember("memberSub");
    List<Tuple> result =
        queryFactory
            .select(member.username, select(memberSub.age.avg()).from(memberSub))
            .from(member)
            .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  @Test
  public void basicCase() {
    List<String> result =
        queryFactory
            .select(member.age.when(10).then("열살").when(20).then("스무살").otherwise("기타"))
            .from(member)
            .fetch();
    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  public void complexCase() {
    List<String> result =
        queryFactory
            .select(
                new CaseBuilder()
                    .when(member.age.between(0, 20))
                    .then("0 ~ 20")
                    .when(member.age.between(21, 30))
                    .then("21 ~ 30")
                    .otherwise("기타"))
            .from(member)
            .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  public void orderByWithCase() {
    NumberExpression<Integer> rankPath =
        new CaseBuilder()
            .when(member.age.between(0, 20))
            .then(2)
            .when(member.age.between(21, 30))
            .then(1)
            .otherwise(3);

    List<Tuple> result =
        queryFactory
            .select(member.username, member.age, rankPath)
            .from(member)
            .orderBy(rankPath.desc())
            .fetch();

    for (Tuple tuple : result) {
      String username = tuple.get(member.username);
      Integer age = tuple.get(member.age);
      Integer rank = tuple.get(rankPath);
      System.out.println("username = " + username + " age = " + age + " rank = " + rank);
    }
  }

  @Test
  public void constant() {
    List<Tuple> result =
        queryFactory.select(member.username, Expressions.constant("A")).from(member).fetch();
    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  @Test
  public void concat() {
    List<String> result =
        queryFactory
            .select(member.username.concat("_").concat(member.age.stringValue()))
            .from(member)
            .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  public void simpleProjection() {
    List<String> result = queryFactory.select(member.username).from(member).fetch();
    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  public void tupleProjection() {
    List<Tuple> result = queryFactory.select(member.username, member.age).from(member).fetch();

    for (Tuple tuple : result) {
      String username = tuple.get(member.username);
      Integer age = tuple.get(member.age);
      System.out.println("username = " + username);
      System.out.println("age = " + age);
    }
  }

  @Test
  public void findDtoByJPQL() {
    List<MemberDto> resultList =
        em.createQuery(
                "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m",
                MemberDto.class)
            .getResultList();

    for (MemberDto memberDto : resultList) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  public void findDtoBySetter() {
    List<MemberDto> result =
        queryFactory
            .select(Projections.bean(MemberDto.class, member.username, member.age))
            .from(member)
            .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  public void findDtoByField() {
    List<MemberDto> result =
        queryFactory
            .select(Projections.fields(MemberDto.class, member.username, member.age))
            .from(member)
            .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  public void findDtoByConstructor() {
    List<MemberDto> result =
        queryFactory
            .select(Projections.constructor(MemberDto.class, member.username, member.age))
            .from(member)
            .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  public void findUserDtoByField() {
    QMember memberSub = new QMember("memberSub");

    List<UserDto> result =
        queryFactory
            .select(
                Projections.fields(
                    UserDto.class,
                    member.username.as("name"),
                    ExpressionUtils.as(
                        JPAExpressions.select(memberSub.age.max()).from(memberSub), "age")))
            .from(member)
            .fetch();
    for (UserDto userDto : result) {
      System.out.println("userDto = " + userDto);
    }
  }

  @Test
  public void findDtoByQueryProjection() {
    List<MemberDto> result =
        queryFactory.select(new QMemberDto(member.username, member.age)).from(member).fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }
}
