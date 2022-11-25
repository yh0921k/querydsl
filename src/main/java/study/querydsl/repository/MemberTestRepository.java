package study.querydsl.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.repository.support.Querydsl4RepositorySupport;

import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {

  public MemberTestRepository() {
    super(Member.class);
  }

  public List<Member> basicSelect() {
    return select(member).from(member).fetch();
  }

  public List<Member> basicSelectFrom() {
    return selectFrom(member).fetch();
  }

  public Page<Member> searchPageByApplyPage(MemberSearchCondition condition, Pageable pageable) {
    JPAQuery<Member> jpaQuery =
        selectFrom(member)
            .leftJoin(member.team, team)
            .where(
                usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()));

    // QuerydslRepositorySupport 방식
    List<Member> content = getQuerydsl().applyPagination(pageable, jpaQuery).fetch();

    return PageableExecutionUtils.getPage(content, pageable, jpaQuery::fetchCount);
  }

  public Page<Member> applyMyPagination(MemberSearchCondition condition, Pageable pageable) {
    return applyPagination(
        pageable,
        query ->
            query
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(
                    usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())));
  }

  public Page<Member> applyMyPagination2(MemberSearchCondition condition, Pageable pageable) {
    return applyPagination(
        pageable,
        query ->
            query
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(
                    usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())),
        countQuery ->
            countQuery
                .select(member.id)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                    usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())));
  }

  private BooleanExpression ageLoe(Integer ageLoe) {
    return ageLoe != null ? member.age.loe(ageLoe) : null;
  }

  private BooleanExpression ageGoe(Integer ageGoe) {
    return ageGoe != null ? member.age.goe(ageGoe) : null;
  }

  private Predicate teamNameEq(String teamName) {
    return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
  }

  private Predicate usernameEq(String username) {
    return StringUtils.hasText(username) ? member.username.eq(username) : null;
  }
}
