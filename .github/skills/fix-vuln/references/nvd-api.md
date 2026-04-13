# NVD API Reference

## Endpoint

```
GET https://services.nvd.nist.gov/rest/json/cves/2.0?cveId=CVE-XXXX-XXXXX
```

No API key required for low-volume use (rate limit: ~5 req/30 s). Add header `apiKey: <key>` to increase to 50 req/30 s.

## Minimal request (PowerShell)

```powershell
$r = Invoke-RestMethod "https://services.nvd.nist.gov/rest/json/cves/2.0?cveId=CVE-2023-46604"
$vuln = $r.vulnerabilities[0].cve
```

## Key fields to extract

| Field path | Meaning |
|---|---|
| `$vuln.id` | CVE ID |
| `$vuln.descriptions` | Array of `{lang, value}` — use `lang == "en"` for description |
| `$vuln.configurations[].nodes[].cpeMatch[]` | CPE entries identifying affected software |
| `$vuln.configurations[].nodes[].cpeMatch[].criteria` | CPE URI, e.g. `cpe:2.3:a:apache:activemq:*:*:*:*:*:*:*:*` |
| `$vuln.configurations[].nodes[].cpeMatch[].versionStartIncluding` | Vulnerable from |
| `$vuln.configurations[].nodes[].cpeMatch[].versionEndExcluding` | Fixed in (exclusive upper bound — this is the safe version) |
| `$vuln.configurations[].nodes[].cpeMatch[].versionEndIncluding` | Vulnerable up to and including this version |

## CPE URI structure

```
cpe:2.3:a:<vendor>:<product>:<version>:...
```

- Part `a` = application, `o` = OS, `h` = hardware
- `vendor` and `product` together identify the library (e.g., `org.apache.activemq` / `activemq`, `com.fasterxml.jackson.core` / `jackson-databind`)

## Deriving the safe version

1. Collect all `cpeMatch` entries where `vulnerable == true`.
2. The safe version is the value in `versionEndExcluding` (if present), or one patch above `versionEndIncluding`.
3. If multiple ranges exist, find the patch/minor version immediately above the highest upper bound.

## Example: CVE-2023-46604 (Apache ActiveMQ RCE)

```json
{
  "cve": {
    "id": "CVE-2023-46604",
    "descriptions": [{ "lang": "en", "value": "Apache ActiveMQ RCE..." }],
    "configurations": [{
      "nodes": [{
        "cpeMatch": [{
          "vulnerable": true,
          "criteria": "cpe:2.3:a:apache:activemq:*:*:*:*:*:*:*:*",
          "versionStartIncluding": "5.0.0",
          "versionEndExcluding": "5.15.16"
        }, {
          "vulnerable": true,
          "criteria": "cpe:2.3:a:apache:activemq:*:*:*:*:*:*:*:*",
          "versionStartIncluding": "5.16.0",
          "versionEndExcluding": "5.16.7"
        }]
      }]
    }]
  }
}
```

Safe version: `5.16.7` (or the latest 5.x patch release >= that bound).

## Fallback: NVD web page

If the API returns no `configurations` (some CVEs lack CPE data), fetch the human-readable page:

```
https://nvd.nist.gov/vuln/detail/<CVE-ID>
```

Use `web_fetch` to retrieve the page and extract the affected software table.

## Maven Central / package registries — finding latest safe version

Once the safe version floor is known, confirm the version exists and find the latest patch in that line:

- **Maven/Gradle**: `https://search.maven.org/solrsearch/select?q=g:<groupId>+AND+a:<artifactId>&rows=20&wt=json`
- **npm**: `https://registry.npmjs.org/<package>`  (`.versions` keys list all published versions)
- **PyPI**: `https://pypi.org/pypi/<package>/json`  (`.releases` keys)
- **Go**: `https://proxy.golang.org/<module>/@v/list`
