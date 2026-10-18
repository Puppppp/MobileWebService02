
from django.test import TestCase
from rest_framework.test import APIClient
from rest_framework import status

from blog.models import Post


class PhotoBlogAPITest(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.url = "/api_root/Post/"

    def test_get_posts_returns_200(self):
        response = self.client.get(self.url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_get_posts_returns_a_list(self):
        response = self.client.get(self.url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data, list)

    def test_post_data_contains_image_field(self):
        response = self.client.get(self.url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        if response.data:
            self.assertIn("image", response.data[0])
