package org.sonar.plugins.clirr;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;

import java.util.Arrays;
import java.util.List;

public class ClirrDecorator implements Decorator {

  private final RulesProfile rulesProfile;
  private final ClirrRulesRepository rulesRepo;

  public ClirrDecorator(RulesProfile rulesProfile, ClirrRulesRepository rulesRepo) {
    this.rulesProfile = rulesProfile;
    this.rulesRepo = rulesRepo;
  }

  @DependedUpon
  public List<Metric> generatesMetrics() {
    return Arrays.asList(ClirrMetrics.TOTAL_API_CHANGES,
        ClirrMetrics.API_BREAKS,
        ClirrMetrics.API_BEHAVIOR_CHANGES,
        ClirrMetrics.NEW_API);
  }

  /**
   * {@inheritDoc}
   */
  public boolean shouldExecuteOnProject(Project project) {
    return project.getLanguage().equals(Java.INSTANCE);
  }

  public void decorate(Resource resource, DecoratorContext context) {
    int apiBreaks = MeasureUtils.sum(true, context.getChildrenMeasures(ClirrMetrics.API_BREAKS)).intValue();
    int apiBehaviorChanges = MeasureUtils.sum(true, context.getChildrenMeasures(ClirrMetrics.API_BEHAVIOR_CHANGES)).intValue();
    int newApi = MeasureUtils.sum(true, context.getChildrenMeasures(ClirrMetrics.NEW_API)).intValue();

    System.out.println("violations for " + resource + ": " + context.getViolations().size());
    for (Violation violation : context.getViolations()) {
      System.out.println("rule api break from repo: " + rulesRepo.getApiBreakRule());
      System.out.println("violation rule: " + violation.getRule());
      System.out.println("equals: " + violation.getRule().equals(rulesRepo.getApiBreakRule()));
      if (violation.getRule().equals(rulesRepo.getApiBreakRule())) {
        apiBreaks++;
        System.out.println("apibreak++");
      } else if (violation.getRule().equals(rulesRepo.getApiBehaviorChangeRule())) {
        apiBehaviorChanges++;
      } else if (violation.getRule().equals(rulesRepo.getNewApiRule())) {
        newApi++;
      }
    }

    context.saveMeasure(ClirrMetrics.API_BREAKS, (double) apiBreaks);
    context.saveMeasure(ClirrMetrics.API_BEHAVIOR_CHANGES, (double) apiBehaviorChanges);
    context.saveMeasure(ClirrMetrics.NEW_API, (double) newApi);
    context.saveMeasure(ClirrMetrics.TOTAL_API_CHANGES, (double) (apiBreaks + apiBehaviorChanges + newApi));
  }
}
