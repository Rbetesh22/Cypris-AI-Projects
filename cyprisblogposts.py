import pandas as pd
from bs4 import BeautifulSoup

def add_anchors_to_sections(html_content):
    if not isinstance(html_content, str):
        return html_content

    soup = BeautifulSoup(html_content, 'html.parser')

    toc_links = soup.find_all('a', href=True)

    for link in toc_links:
        section_id = link['href'].replace('#', '')
        section_name = link.text
        if section_name.lower() == 'conclusion':
            break

        heading = soup.find(lambda tag: tag.name in ['h1', 'h2', 'h3', 'h4', 'h5', 'h6'] and tag.text.strip() == section_name)
        if heading is not None:
            # Create a new anchor tag
            anchor_tag = soup.new_tag('a', attrs={'name': section_id})

            # Replace heading text with anchor tag + text
            heading.string.wrap(anchor_tag)

    return str(soup)


def add_anchors_to_csv(input_csv, output_csv):
    df = pd.read_csv(input_csv)
    df['Post Body'] = df['Post Body'].apply(add_anchors_to_sections)
    df.to_csv(output_csv, index=False)

add_anchors_to_csv('cyprisblogposts.csv', 'modifiedcyprisblogposts5.csv')

