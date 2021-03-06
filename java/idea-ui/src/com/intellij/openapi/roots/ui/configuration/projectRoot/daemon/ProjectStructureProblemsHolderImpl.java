package com.intellij.openapi.roots.ui.configuration.projectRoot.daemon;

import com.intellij.util.SmartList;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ProjectStructureProblemsHolderImpl implements ProjectStructureProblemsHolder {
  private List<ProjectStructureProblemDescription> myProblemDescriptions;

  @Override
  public void registerProblem(@NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String message, @Nullable String description,
                              @NotNull ProjectStructureProblemType problemType,
                              @NotNull PlaceInProjectStructure place,
                              @Nullable ConfigurationErrorQuickFix fix) {
    final List<ConfigurationErrorQuickFix> fixes = fix != null ? Collections.singletonList(fix) : Collections.emptyList();
    registerProblem(new ProjectStructureProblemDescription(message, description, place, problemType, fixes));
  }

  @Override
  public void registerProblem(final @NotNull ProjectStructureProblemDescription description) {
    if (myProblemDescriptions == null) {
      myProblemDescriptions = new SmartList<>();
    }
    myProblemDescriptions.add(description);
  }

  public String composeTooltipMessage() {
    final StringBuilder buf = new StringBuilder();
    buf.append("<html><body>");
    if (myProblemDescriptions != null) {
      int problems = 0;
      for (ProjectStructureProblemDescription problemDescription : myProblemDescriptions) {
        buf.append(XmlStringUtil.convertToHtmlContent(problemDescription.getMessage(false))).append("<br>");
        problems++;
        if (problems >= 10 && myProblemDescriptions.size() > 12) {
          buf.append(myProblemDescriptions.size() - problems).append(" more problems...<br>");
          break;
        }
      }
    }
    buf.append("</body></html>");
    return buf.toString();
  }

  public boolean containsProblems() {
    return myProblemDescriptions != null && !myProblemDescriptions.isEmpty();
  }

  public boolean containsProblems(final ProjectStructureProblemType.Severity severity) {
    if (myProblemDescriptions != null) {
      for (ProjectStructureProblemDescription description : myProblemDescriptions) {
        if (description.getSeverity() == severity) {
          return true;
        }
      }
    }
    return false;
  }

  public void removeProblem(@NotNull ProjectStructureProblemDescription description) {
    if (myProblemDescriptions != null) {
      myProblemDescriptions.remove(description);
    }
  }

  @Nullable
  public List<ProjectStructureProblemDescription> getProblemDescriptions() {
    return myProblemDescriptions;
  }
}
