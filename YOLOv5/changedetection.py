import cv2
import requests
import os
from pathlib import Path
from datetime import datetime


class ChangeDetection:

    HOST = os.getenv(
        "YOLO_HOST",
        "https://neji.pythonanywhere.com"
    )

    def __init__(self, names):
        self.result_prev = [0 for i in range(len(names))]

        self.token = os.getenv("YOLO_CLOUD_TOKEN")

        if not self.token:
            raise ValueError("YOLO_CLOUD_TOKEN is not configured.")

        print("Cloud authentication token loaded successfully.")
    print("Django authentication successful.")

    def add(self, names, detected_current, save_dir, image):

        self.title = ""
        self.text = ""
        change_flag = 0

        for i in range(len(self.result_prev)):

            if self.result_prev[i] == 0 and detected_current[i] == 1:

                change_flag = 1
                self.title = names[i]
                self.text += names[i] + ", "

        self.result_prev = detected_current[:]

        if change_flag == 1:
            self.send(save_dir, image)

    def send(self, save_dir, image):
        now = datetime.now()
        today_path = (
            Path(save_dir) / "detected"/ str(now.year)/ str(now.month)/ str(now.day)
        )
        today_path.mkdir(parents=True, exist_ok=True)

        filename = (
            f"{now.hour}-{now.minute}-"
            f"{now.second}-{now.microsecond}.jpg"
        )

        full_path = today_path / filename
        # resize
        resized_image = cv2.resize( image, dsize=(320, 240), interpolation=cv2.INTER_AREA)
        cv2.imwrite(str(full_path), resized_image)
        headers = {
            "Authorization": "Token " + self.token, "Accept": "application/json"
        }

        # Post
        data = {
            "author": 1,
            "title": self.title,
            "text": self.text,
            "created_date": now.isoformat(),
            "published_date": now.isoformat()
        }

        # Upload
        with open(full_path, "rb") as image_file:

            files = {
                "image": (
                    filename,
                    image_file,
                    "image/jpeg"
                )
            }

            response = requests.post(
                self.HOST + "/api_root/Post/",
                data=data,
                files=files,
                headers=headers
            )

        print("Django upload status:", response.status_code)
        print("Django response:", response.text)

        response.raise_for_status()
