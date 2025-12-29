import os
import json
import requests
from datetime import datetime, timedelta
from typing import List, Dict, Optional
import time

GITHUB_TOKEN = os.environ.get('GITHUB_TOKEN')
HEADERS = {
    'Authorization': f'token {GITHUB_TOKEN}',
    'Accept': 'application/vnd.github.v3+json'
}

PLATFORMS = {
    'android': {
        'topic': 'android',
        'installer_extensions': ['.apk'],
        'score_keywords': ['android', 'mobile', 'kotlin', 'java', 'apk']
    },
    'windows': {
        'topic': 'desktop',
        'installer_extensions': ['.msi', '.exe'],
        'score_keywords': ['desktop', 'electron', 'windows', 'app', 'gui']
    },
    'macos': {
        'topic': 'macos',
        'installer_extensions': ['.dmg', '.pkg'],
        'score_keywords': ['macos', 'desktop', 'app', 'swift']
    },
    'linux': {
        'topic': 'linux',
        'installer_extensions': ['.appimage', '.deb', '.rpm'],
        'score_keywords': ['linux', 'desktop', 'app']
    }
}

def calculate_platform_score(repo: Dict, platform: str) -> int:
    """Calculate relevance score for a repository"""
    score = 5
    topics = [t.lower() for t in repo.get('topics', [])]
    language = (repo.get('language') or '').lower()
    desc = (repo.get('description') or '').lower()

    keywords = PLATFORMS[platform]['score_keywords']

    for keyword in keywords:
        if keyword in topics:
            score += 10
        if keyword in desc:
            score += 3

    if language in ['kotlin', 'c++', 'rust', 'c#', 'swift', 'dart', 'java']:
        score += 5

    if 'cross-platform' in topics or 'multiplatform' in topics:
        score += 8

    return score

def check_repo_has_installers(owner: str, repo_name: str, platform: str) -> bool:
    """Check if repository has relevant installer files"""
    try:
        url = f'https://api.github.com/repos/{owner}/{repo_name}/releases'
        response = requests.get(url, headers=HEADERS, params={'per_page': 10}, timeout=10)

        if response.status_code != 200:
            return False

        releases = response.json()

        # Find first stable release
        stable_release = None
        for release in releases:
            if not release.get('draft') and not release.get('prerelease'):
                stable_release = release
                break

        if not stable_release or not stable_release.get('assets'):
            return False

        # Check for relevant installer files
        extensions = PLATFORMS[platform]['installer_extensions']
        for asset in stable_release['assets']:
            asset_name = asset['name'].lower()
            if any(asset_name.endswith(ext) for ext in extensions):
                return True

        return False

    except Exception as e:
        print(f"Error checking installers for {owner}/{repo_name}: {e}")
        return False

def fetch_trending_repos(platform: str, desired_count: int = 30) -> List[Dict]:
    """Fetch trending repositories for a specific platform"""
    print(f"\n{'='*60}")
    print(f"Fetching trending repos for {platform.upper()}")
    print(f"{'='*60}")

    # Calculate date 7 days ago
    seven_days_ago = (datetime.utcnow() - timedelta(days=7)).strftime('%Y-%m-%d')

    topic = PLATFORMS[platform]['topic']
    query = f'stars:>500 archived:false pushed:>={seven_days_ago} topic:{topic}'

    print(f"Query: {query}")

    results = []
    page = 1
    max_pages = 5

    while len(results) < desired_count and page <= max_pages:
        print(f"\nFetching API page {page}...")

        try:
            url = 'https://api.github.com/search/repositories'
            params = {
                'q': query,
                'sort': 'stars',
                'order': 'desc',
                'per_page': 100,
                'page': page
            }

            response = requests.get(url, headers=HEADERS, params=params, timeout=30)

            if response.status_code != 200:
                print(f"Error: API returned status {response.status_code}")
                break

            data = response.json()
            items = data.get('items', [])

            print(f"Got {len(items)} repositories from API")

            if not items:
                break

            # Score and filter candidates
            candidates = []
            for repo in items:
                score = calculate_platform_score(repo, platform)
                if score > 0:
                    candidates.append((repo, score))

            # Sort by score and take top 50
            candidates.sort(key=lambda x: x[1], reverse=True)
            candidates = [repo for repo, _ in candidates[:50]]

            print(f"Checking {len(candidates)} candidates for installers...")

            # Check each candidate for installers
            for repo in candidates:
                if len(results) >= desired_count:
                    break

                owner = repo['owner']['login']
                name = repo['name']

                print(f"Checking {owner}/{name}...", end=' ')

                if check_repo_has_installers(owner, name, platform):
                    # Transform to summary format
                    summary = {
                        'id': repo['id'],
                        'name': repo['name'],
                        'fullName': repo['full_name'],
                        'owner': {
                            'login': repo['owner']['login'],
                            'avatarUrl': repo['owner']['avatar_url']
                        },
                        'description': repo.get('description'),
                        'defaultBranch': repo.get('default_branch', 'main'),
                        'htmlUrl': repo['html_url'],
                        'stargazersCount': repo['stargazers_count'],
                        'forksCount': repo['forks_count'],
                        'language': repo.get('language'),
                        'topics': repo.get('topics', []),
                        'releasesUrl': repo['releases_url'],
                        'updatedAt': repo['updated_at']
                    }
                    results.append(summary)
                    print(f"✓ Found ({len(results)}/{desired_count})")
                else:
                    print("✗ No installers")

                # Small delay to avoid rate limiting
                time.sleep(0.5)

            page += 1

        except Exception as e:
            print(f"Error fetching page {page}: {e}")
            break

    print(f"\n{'='*60}")
    print(f"Total found: {len(results)} repositories for {platform}")
    print(f"{'='*60}\n")

    return results

def main():
    """Main function to fetch and save trending repos for all platforms"""
    timestamp = datetime.utcnow().isoformat() + 'Z'

    for platform in PLATFORMS.keys():
        print(f"\nProcessing {platform}...")

        repos = fetch_trending_repos(platform, desired_count=30)

        output = {
            'platform': platform,
            'lastUpdated': timestamp,
            'totalCount': len(repos),
            'repositories': repos
        }

        # Save to file
        output_dir = 'cached-data/trending'
        os.makedirs(output_dir, exist_ok=True)

        output_file = f'{output_dir}/{platform}.json'
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(output, f, indent=2, ensure_ascii=False)

        print(f"✓ Saved {len(repos)} repos to {output_file}")

        # Delay between platforms to avoid rate limiting
        time.sleep(2)

    print("\n✓ All platforms processed successfully!")

if __name__ == '__main__':
    main()