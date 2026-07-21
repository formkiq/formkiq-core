#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';

const args = parseArgs(process.argv.slice(2));
const input = args.input ?? 'build/reports/trivy/trivy-results.json';
const output = args.output ?? 'build/reports/trivy/readable-report.md';
const root = args.root ?? '.';

const trivy = JSON.parse(fs.readFileSync(input, 'utf8'));
const lockfileCache = new Map();

const severityRank = new Map([
  ['CRITICAL', 0],
  ['HIGH', 1],
  ['MEDIUM', 2],
  ['LOW', 3],
  ['UNKNOWN', 4]
]);

const configurationRank = new Map([
  ['runtime', 0],
  ['compile', 1],
  ['integration', 2],
  ['test', 3],
  ['tooling', 4],
  ['unknown', 5]
]);

const vulnerabilityGroups = new Map();
const misconfigurationGroups = new Map();
const licenseGroups = new Map();
let secrets = 0;

for (const result of trivy.Results ?? []) {
  const target = result.Target ?? '';
  const lockfileEntries = target.endsWith('gradle.lockfile') ? readGradleLockfile(target) : new Map();

  for (const vulnerability of result.Vulnerabilities ?? []) {
    const key = [
      vulnerability.VulnerabilityID,
      vulnerability.PkgName,
      vulnerability.InstalledVersion
    ].join('\u0000');
    const group = getOrCreate(vulnerabilityGroups, key, () => ({
      id: vulnerability.VulnerabilityID,
      packageName: vulnerability.PkgName,
      installedVersion: vulnerability.InstalledVersion ?? '',
      fixedVersion: vulnerability.FixedVersion ?? '',
      severity: vulnerability.Severity ?? 'UNKNOWN',
      title: vulnerability.Title ?? '',
      occurrences: 0,
      targets: new Set(),
      configurations: new Set(),
      configurationTypes: new Set()
    }));

    group.occurrences += 1;
    group.targets.add(target);

    const lockfileKey = `${vulnerability.PkgName}:${vulnerability.InstalledVersion}`;
    const configurations = lockfileEntries.get(lockfileKey) ?? ['unknown'];
    for (const configuration of configurations) {
      group.configurations.add(configuration);
      group.configurationTypes.add(configurationType(configuration));
    }
  }

  for (const misconfiguration of result.Misconfigurations ?? []) {
    const key = [
      misconfiguration.ID,
      misconfiguration.Severity,
      misconfiguration.Title
    ].join('\u0000');
    const group = getOrCreate(misconfigurationGroups, key, () => ({
      id: misconfiguration.ID,
      severity: misconfiguration.Severity ?? 'UNKNOWN',
      title: misconfiguration.Title ?? '',
      occurrences: 0,
      targets: new Set()
    }));

    group.occurrences += 1;
    group.targets.add(target);
  }

  for (const secret of result.Secrets ?? []) {
    if (secret) {
      secrets += 1;
    }
  }

  for (const license of result.Licenses ?? []) {
    const severity = license.Severity ?? 'UNKNOWN';
    const name = license.Name ?? 'UNKNOWN';
    const key = `${severity}\u0000${name}`;
    const group = getOrCreate(licenseGroups, key, () => ({
      severity,
      name,
      occurrences: 0
    }));
    group.occurrences += 1;
  }
}

const vulnerabilities = [...vulnerabilityGroups.values()].sort(compareVulnerabilities);
const misconfigurations = [...misconfigurationGroups.values()].sort(compareSeverityAndOccurrences);
const licenses = [...licenseGroups.values()].sort(compareSeverityAndOccurrences);

const lines = [];
lines.push('# Trivy Repository Scan Report');
lines.push('');
lines.push(`Generated from \`${input}\`.`);
lines.push('');
lines.push(`- Scan time: \`${trivy.CreatedAt ?? 'unknown'}\``);
lines.push(`- Trivy version: \`${trivy.ArtifactName ? 'reported in JSON metadata' : 'unknown'}\``);
lines.push('- Enabled scanners: `vuln`');
lines.push('- Java dependency source: built JAR artifacts and collected Gradle runtime classpaths');
lines.push('- Configuration priority: runtime, compile, integration, test, tooling, unknown');
lines.push('');

lines.push('## Executive Summary');
lines.push('');
lines.push('| Finding type | Critical | High | Medium | Low | Unknown | Total |');
lines.push('| --- | ---: | ---: | ---: | ---: | ---: | ---: |');
lines.push(summaryRow('Vulnerabilities, raw findings', flattenVulnerabilities()));
lines.push(summaryRow('Vulnerabilities, unique package/version/CVE', vulnerabilities));
lines.push(summaryRow('Misconfigurations', flattenMisconfigurations()));
lines.push(summaryRow('Secrets', Array.from({ length: secrets }, () => ({ severity: 'UNKNOWN' }))));
lines.push(summaryRow('License findings', flattenLicenses()));
lines.push('');

lines.push('## Vulnerabilities By Configuration');
lines.push('');
lines.push('| Configuration type | Critical | High | Medium | Low | Unknown | Unique findings |');
lines.push('| --- | ---: | ---: | ---: | ---: | ---: | ---: |');
for (const type of ['runtime', 'compile', 'integration', 'test', 'tooling', 'unknown']) {
  const matching = vulnerabilities.filter((vulnerability) => vulnerability.configurationTypes.has(type));
  lines.push(summaryRow(type, matching, 'Unique findings'));
}
lines.push('');
lines.push('Runtime and compile findings should be triaged first. Test, integration, and tooling findings may still matter, but they usually do not ship in the production artifact.');
lines.push('');

lines.push('## Runtime And Compile Findings');
lines.push('');
lines.push('| Severity | Config type | CVE | Package | Installed | Fixed | Configurations | Action |');
lines.push('| --- | --- | --- | --- | --- | --- | --- | --- |');
for (const vulnerability of vulnerabilities.filter(hasRuntimeOrCompile).slice(0, 30)) {
  lines.push(vulnerabilityRow(vulnerability));
}
if (!vulnerabilities.some(hasRuntimeOrCompile)) {
  lines.push('| None |  |  |  |  |  |  |  |');
}
lines.push('');

lines.push('## Test And Integration Findings');
lines.push('');
lines.push('| Severity | Config type | CVE | Package | Installed | Fixed | Configurations | Action |');
lines.push('| --- | --- | --- | --- | --- | --- | --- | --- |');
for (const vulnerability of vulnerabilities.filter(isTestOrIntegrationOnly).slice(0, 30)) {
  lines.push(vulnerabilityRow(vulnerability));
}
if (!vulnerabilities.some(isTestOrIntegrationOnly)) {
  lines.push('| None |  |  |  |  |  |  |  |');
}
lines.push('');

lines.push('## Tooling Findings');
lines.push('');
lines.push('| Severity | CVE | Package | Installed | Fixed | Occurrences | Action |');
lines.push('| --- | --- | --- | --- | --- | ---: | --- |');
for (const vulnerability of vulnerabilities.filter(isToolingOnly).slice(0, 30)) {
  lines.push([
    vulnerability.severity,
    code(vulnerability.id),
    code(vulnerability.packageName),
    code(vulnerability.installedVersion),
    code(vulnerability.fixedVersion || 'not listed'),
    vulnerability.occurrences,
    remediation(vulnerability)
  ].join(' | ').replace(/^/, '| ').replace(/$/, ' |'));
}
if (!vulnerabilities.some(isToolingOnly)) {
  lines.push('| None |  |  |  |  |  |  |');
}
lines.push('');

lines.push('## Misconfigurations');
lines.push('');
lines.push('| Severity | Count | Rule | Title | Example target |');
lines.push('| --- | ---: | --- | --- | --- |');
for (const misconfiguration of misconfigurations.slice(0, 20)) {
  lines.push([
    misconfiguration.severity,
    misconfiguration.occurrences,
    code(misconfiguration.id),
    escapePipe(misconfiguration.title),
    code([...misconfiguration.targets][0] ?? '')
  ].join(' | ').replace(/^/, '| ').replace(/$/, ' |'));
}
lines.push('');

lines.push('## License Findings');
lines.push('');
lines.push('| Severity | Count | License |');
lines.push('| --- | ---: | --- |');
for (const license of licenses.slice(0, 20)) {
  lines.push([
    license.severity,
    license.occurrences,
    code(license.name)
  ].join(' | ').replace(/^/, '| ').replace(/$/, ' |'));
}
lines.push('');

lines.push('## How To Trace A Finding');
lines.push('');
lines.push('Use Gradle dependency insight against a representative affected module and configuration:');
lines.push('');
lines.push('```bash');
lines.push('./gradlew :apps:lambda-api:dependencyInsight --configuration runtimeClasspath --dependency <artifact>');
lines.push('./gradlew :apps:lambda-api:dependencyInsight --configuration testRuntimeClasspath --dependency <artifact>');
lines.push('./gradlew dependencyInsight --configuration checkstyle --dependency <artifact>');
lines.push('```');
lines.push('');
lines.push('For example, `CVE-2025-48734` is `commons-beanutils:commons-beanutils:1.9.4` on the `checkstyle` configuration. It comes from `com.puppycrawl.tools:checkstyle:10.12.4`, so the useful fix is to update the Checkstyle tool version or add a Checkstyle classpath constraint for `commons-beanutils:commons-beanutils:1.11.0`; the individual module lockfiles are only symptoms.');

fs.mkdirSync(path.dirname(output), { recursive: true });
fs.writeFileSync(output, `${lines.join('\n')}\n`);

function parseArgs(argv) {
  const parsed = {};
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg.startsWith('--')) {
      parsed[arg.slice(2)] = argv[i + 1];
      i += 1;
    }
  }
  return parsed;
}

function readGradleLockfile(target) {
  if (lockfileCache.has(target)) {
    return lockfileCache.get(target);
  }

  const file = path.resolve(root, target);
  const entries = new Map();
  if (!fs.existsSync(file)) {
    lockfileCache.set(target, entries);
    return entries;
  }

  for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    if (!line || line.startsWith('#') || line.startsWith('empty=')) {
      continue;
    }

    const separator = line.indexOf('=');
    if (separator === -1) {
      continue;
    }

    const coordinate = line.slice(0, separator);
    const coordinateParts = coordinate.split(':');
    if (coordinateParts.length < 3) {
      continue;
    }

    const packageName = `${coordinateParts[0]}:${coordinateParts[1]}`;
    const version = coordinateParts.slice(2).join(':');
    const configurations = line.slice(separator + 1).split(',').filter(Boolean);
    entries.set(`${packageName}:${version}`, configurations);
  }

  lockfileCache.set(target, entries);
  return entries;
}

function getOrCreate(map, key, create) {
  if (!map.has(key)) {
    map.set(key, create());
  }
  return map.get(key);
}

function configurationType(configuration) {
  const normalized = configuration.toLowerCase();
  if (normalized === 'runtimeclasspath' || normalized === 'runtimeonly' || normalized === 'implementation') {
    return 'runtime';
  }
  if (normalized === 'compileclasspath' || normalized === 'compileonly' || normalized === 'api') {
    return 'compile';
  }
  if (normalized.includes('integration')) {
    return 'integration';
  }
  if (normalized.includes('test')) {
    return 'test';
  }
  if (normalized === 'checkstyle' || normalized.includes('spotbugs') || normalized.includes('pmd')) {
    return 'tooling';
  }
  return 'unknown';
}

function compareVulnerabilities(left, right) {
  return configurationPriority(left) - configurationPriority(right)
    || severityPriority(left.severity) - severityPriority(right.severity)
    || right.occurrences - left.occurrences
    || left.id.localeCompare(right.id);
}

function compareSeverityAndOccurrences(left, right) {
  return severityPriority(left.severity) - severityPriority(right.severity)
    || right.occurrences - left.occurrences;
}

function configurationPriority(vulnerability) {
  return Math.min(...[...vulnerability.configurationTypes].map((type) => configurationRank.get(type) ?? 5));
}

function severityPriority(severity) {
  return severityRank.get(severity ?? 'UNKNOWN') ?? 4;
}

function flattenVulnerabilities() {
  return [...vulnerabilityGroups.values()].flatMap((group) => (
    Array.from({ length: group.occurrences }, () => ({ severity: group.severity }))
  ));
}

function flattenMisconfigurations() {
  return [...misconfigurationGroups.values()].flatMap((group) => (
    Array.from({ length: group.occurrences }, () => ({ severity: group.severity }))
  ));
}

function flattenLicenses() {
  return [...licenseGroups.values()].flatMap((group) => (
    Array.from({ length: group.occurrences }, () => ({ severity: group.severity }))
  ));
}

function summaryRow(label, findings, totalLabel = 'Total') {
  const counts = countBySeverity(findings);
  const total = findings.length;
  if (totalLabel === 'Unique findings') {
    return `| ${label} | ${counts.CRITICAL} | ${counts.HIGH} | ${counts.MEDIUM} | ${counts.LOW} | ${counts.UNKNOWN} | ${total} |`;
  }
  return `| ${label} | ${counts.CRITICAL} | ${counts.HIGH} | ${counts.MEDIUM} | ${counts.LOW} | ${counts.UNKNOWN} | ${total} |`;
}

function countBySeverity(findings) {
  const counts = {
    CRITICAL: 0,
    HIGH: 0,
    MEDIUM: 0,
    LOW: 0,
    UNKNOWN: 0
  };
  for (const finding of findings) {
    counts[finding.severity ?? 'UNKNOWN'] = (counts[finding.severity ?? 'UNKNOWN'] ?? 0) + 1;
  }
  return counts;
}

function hasRuntimeOrCompile(vulnerability) {
  return vulnerability.configurationTypes.has('runtime') || vulnerability.configurationTypes.has('compile');
}

function isTestOrIntegrationOnly(vulnerability) {
  return !hasRuntimeOrCompile(vulnerability)
    && !vulnerability.configurationTypes.has('tooling')
    && (vulnerability.configurationTypes.has('test') || vulnerability.configurationTypes.has('integration'));
}

function isToolingOnly(vulnerability) {
  return vulnerability.configurationTypes.size === 1 && vulnerability.configurationTypes.has('tooling');
}

function vulnerabilityRow(vulnerability) {
  return [
    vulnerability.severity,
    [...vulnerability.configurationTypes].sort((a, b) => configurationRank.get(a) - configurationRank.get(b)).join(', '),
    code(vulnerability.id),
    code(vulnerability.packageName),
    code(vulnerability.installedVersion),
    code(vulnerability.fixedVersion || 'not listed'),
    code([...vulnerability.configurations].sort().join(', ')),
    remediation(vulnerability)
  ].join(' | ').replace(/^/, '| ').replace(/$/, ' |');
}

function remediation(vulnerability) {
  const fixedVersion = vulnerability.fixedVersion?.split(',')[0]?.trim();
  if (vulnerability.configurationTypes.has('tooling') && vulnerability.configurations.has('checkstyle')) {
    if (vulnerability.packageName === 'commons-beanutils:commons-beanutils') {
      return 'Update the Checkstyle tool classpath so it resolves `commons-beanutils:commons-beanutils:1.11.0` or newer.';
    }
    if (vulnerability.packageName === 'org.codehaus.plexus:plexus-utils') {
      return 'Update the Checkstyle tool classpath so it resolves `org.codehaus.plexus:plexus-utils:3.6.1` or newer.';
    }
    return 'Update the Checkstyle tool version or add a dependency constraint on the Checkstyle configuration.';
  }

  if (vulnerability.configurationTypes.has('runtime') || vulnerability.configurationTypes.has('compile')) {
    return fixedVersion
      ? `Update the direct dependency, BOM, or transitive parent so this resolves to \`${fixedVersion}\` or newer.`
      : 'Update the direct dependency, BOM, or transitive parent that brings this into runtime/compile classpath.';
  }

  if (vulnerability.configurationTypes.has('integration') || vulnerability.configurationTypes.has('test')) {
    return fixedVersion
      ? `Test/integration scope: update the test dependency or transitive parent so this resolves to \`${fixedVersion}\` or newer.`
      : 'Test/integration scope: update the test dependency or transitive parent.';
  }

  return fixedVersion
    ? `Resolve to \`${fixedVersion}\` or newer.`
    : 'Run `dependencyInsight` for the package to identify the parent dependency.';
}

function code(value) {
  return `\`${escapePipe(value)}\``;
}

function escapePipe(value) {
  return String(value ?? '').replace(/\|/g, '\\|');
}
