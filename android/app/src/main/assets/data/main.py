from bs4 import BeautifulSoup
import requests
import json
from tqdm.auto import tqdm
import time


class GenshinDataScraper:
    @staticmethod
    def clean_string(string: str):
        return string.replace("'", "").replace("-", " ").replace("(", "").replace(")", "").replace(":", "")

    @staticmethod
    def convert_to_pascal_case(string: str):
        cleaned_string = GenshinDataScraper.clean_string(string)
        split = cleaned_string.split(" ")
        result = []
        for word in split:
            result.append(word.title())
        return "".join(result)

    @staticmethod
    def scrape_weapons():
        start_time = time.time()
        print("# ################################## #")
        print("#     Starting Weapon Scraping...    #\n")
        html = requests.get("https://github.com/theBowja/genshin-db/tree/main/src/data/English/weapons")
        soup = BeautifulSoup(html.content, "html.parser")

        # Find all <a> tags that holds the link to each JSON file.
        links = soup.find_all("a", class_ = "js-navigation-open Link--primary")

        # Go through each <a> tag and filter out their link and collect them all into a list.
        clean_links = []
        for link in links:
            if "prizedisshinblade" in link["href"]:
                continue

            formatted_link = link["href"].replace("/blob", "")
            clean_links.append("https://raw.githubusercontent.com" + formatted_link)

        # Now visit each link and read each JSON file.
        weapon_data = []
        data_number = 0
        for link in tqdm(clean_links):
            try:
                data_number += 1
                response = requests.get(link)
                json_data = json.loads(response.text)
                weapon_data.append({
                    "name": GenshinDataScraper.convert_to_pascal_case(json_data["name"]),
                    "rarity": json_data["rarity"]
                })
            except Exception:
                continue

        print(f"\nWeapon scraping took: {round(time.time() - start_time, 2)} seconds")

        # Now save the data into a JSON file.
        print("\nNow saving Weapons data into weapons.json file...")
        with open("weapons.json", "w", encoding = "utf-8") as file:
            json.dump(weapon_data, file, indent = 4)
            print(f"JSON file saved successfully with {data_number} weapons.")

        print("\n# ################################## #\n\n")

    @staticmethod
    def scrape_materials():
        start_time = time.time()
        print("# ################################## #")
        print("#     Starting Material Scraping...    #\n")
        html = requests.get("https://github.com/theBowja/genshin-db/tree/main/src/data/English/materials")
        soup = BeautifulSoup(html.content, "html.parser")

        # Find all <a> tags that holds the link to each JSON file.
        links = soup.find_all("a", class_ = "js-navigation-open Link--primary")

        # Go through each <a> tag and filter out their link and collect them all into a list.
        clean_links = []
        for link in links:
            formatted_link = link["href"].replace("/blob", "")
            clean_links.append("https://raw.githubusercontent.com" + formatted_link)

        # Now visit each link and read each JSON file.
        data = []
        data_number = 0
        for link in tqdm(clean_links):
            try:
                data_number += 1
                response = requests.get(link)
                json_data = json.loads(response.text)
                data.append({
                    "name": GenshinDataScraper.convert_to_pascal_case(json_data["name"]),
                })
            except Exception:
                continue

        print(f"\nMaterial scraping took: {round(time.time() - start_time, 2)} seconds")

        # Now save the data into a JSON file.
        print("\nNow saving Materials data into materials.json file...")
        with open("materials.json", "w", encoding = "utf-8") as file:
            json.dump(data, file, indent = 4)
            print(f"JSON file saved successfully with {data_number} materials.")

        print("\n# ################################## #\n\n")

    @staticmethod
    def scrape_character_development_items():
        start_time = time.time()
        print("# ################################## #")
        print("#     Starting Character Development Item Scraping...    #\n")
        html = requests.get("https://github.com/theBowja/genshin-db/tree/main/src/data/English/crafts")
        soup = BeautifulSoup(html.content, "html.parser")

        # Find all <a> tags that holds the link to each JSON file.
        links = soup.find_all("a", class_ = "js-navigation-open Link--primary")

        # Go through each <a> tag and filter out their link and collect them all into a list.
        clean_links = []
        for link in links:
            formatted_link = link["href"].replace("/blob", "")
            clean_links.append("https://raw.githubusercontent.com" + formatted_link)

        # Now visit each link and read each JSON file.
        data = []
        data_number = 0
        for link in tqdm(clean_links):
            try:
                data_number += 1
                response = requests.get(link)
                json_data = json.loads(response.text)
                data.append({
                    "name": GenshinDataScraper.convert_to_pascal_case(json_data["name"]),
                })
            except Exception:
                continue

        print(f"\nCharacter Development Item scraping took: {round(time.time() - start_time, 2)} seconds")

        # Now save the data into a JSON file.
        print("\nNow saving Character Development Items data into characterdevelopmentitems.json file...")
        with open("characterdevelopmentitems.json", "w", encoding = "utf-8") as file:
            json.dump(data, file, indent = 4)
            print(f"JSON file saved successfully with {data_number} character development items.")

        print("\n# ################################## #\n\n")

    @staticmethod
    def scrape_artifacts():
        start_time = time.time()
        print("# ################################## #")
        print("#     Starting Artifact Scraping...    #\n")
        html = requests.get("https://github.com/theBowja/genshin-db/tree/main/src/data/English/artifacts")
        soup = BeautifulSoup(html.content, "html.parser")

        # Find all <a> tags that holds the link to each JSON file.
        links = soup.find_all("a", class_ = "js-navigation-open Link--primary")

        # Go through each <a> tag and filter out their link and collect them all into a list.
        clean_links = []
        for link in links:
            formatted_link = link["href"].replace("/blob", "")
            clean_links.append("https://raw.githubusercontent.com" + formatted_link)

        # Now visit each link and read each JSON file.
        data = []
        data_number = 0
        for link in tqdm(clean_links):
            try:
                data_number += 1
                response = requests.get(link)
                json_data = json.loads(response.text)
                data.append({
                    "name": GenshinDataScraper.convert_to_pascal_case(json_data["name"]),
                    "flower": GenshinDataScraper.convert_to_pascal_case(json_data["flower"]["name"]),
                    "plume": GenshinDataScraper.convert_to_pascal_case(json_data["plume"]["name"]),
                    "sands": GenshinDataScraper.convert_to_pascal_case(json_data["sands"]["name"]),
                    "goblet": GenshinDataScraper.convert_to_pascal_case(json_data["goblet"]["name"]),
                    "circlet": GenshinDataScraper.convert_to_pascal_case(json_data["circlet"]["name"]),
                })
            except Exception:
                continue

        print(f"\nArtifact scraping took: {round(time.time() - start_time, 2)} seconds")

        # Now save the data into a JSON file.
        print("\nNow saving Artifacts data into artifacts.json file...")
        with open("artifacts.json", "w", encoding = "utf-8") as file:
            json.dump(data, file, indent = 4)
            print(f"JSON file saved successfully with {data_number} artifact sets.")

        print("\n# ################################## #\n\n")

    @staticmethod
    def scrape_characters():
        start_time = time.time()
        print("# ################################## #")
        print("#     Starting Characters Scraping...    #\n")
        html = requests.get("https://github.com/theBowja/genshin-db/tree/main/src/data/English/characters")
        soup = BeautifulSoup(html.content, "html.parser")

        # Find all <a> tags that holds the link to each JSON file.
        links = soup.find_all("a", class_ = "js-navigation-open Link--primary")

        # Go through each <a> tag and filter out their link and collect them all into a list.
        clean_links = []
        for link in links:
            formatted_link = link["href"].replace("/blob", "")
            clean_links.append("https://raw.githubusercontent.com" + formatted_link)

        # Now visit each link and read each JSON file.
        data = []
        data_number = 0
        for link in tqdm(clean_links):
            try:
                data_number += 1
                response = requests.get(link)
                json_data = json.loads(response.text)
                data.append({
                    "name": GenshinDataScraper.convert_to_pascal_case(json_data["name"]),
                })
            except Exception:
                continue

        print(f"\nCharacter scraping took: {round(time.time() - start_time, 2)} seconds")

        # Now save the data into a JSON file.
        print("\nNow saving Character data into characters.json file...")
        with open("characters.json", "w", encoding = "utf-8") as file:
            json.dump(data, file, indent = 4)
            print(f"JSON file saved successfully with {data_number} characters.")

        print("\n# ################################## #\n\n")


if __name__ == '__main__':
    start = time.time()

    GenshinDataScraper.scrape_weapons()
    GenshinDataScraper.scrape_materials()
    GenshinDataScraper.scrape_character_development_items()
    GenshinDataScraper.scrape_artifacts()
    GenshinDataScraper.scrape_characters()

    print(f"Total running time: {round(time.time() - start, 2)} seconds")
